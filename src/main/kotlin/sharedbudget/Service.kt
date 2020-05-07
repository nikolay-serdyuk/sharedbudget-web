package sharedbudget

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sharedbudget.entities.ExpenseEntity
import sharedbudget.entities.ExpenseDto
import sharedbudget.entities.ExpensesRepository
import sharedbudget.entities.SpendingDto
import sharedbudget.entities.updateAllFrom
import java.text.SimpleDateFormat
import java.time.Instant
import javax.ws.rs.NotFoundException
import kotlin.random.Random

@Service
@Transactional
class Service(
    private val accountResolver: AccountResolver,
    private val expensesRepository: ExpensesRepository,
    private val locks: Locks
) {

    @Transactional(readOnly = true)
    fun getExpenses(date: String? = null): Iterable<ExpenseEntity> = locks.retryWithLock(accountResolver.accountId) {

        val instantDate = when (date) {
            null -> Instant.now()
            else -> DATE_FORMAT.parse(date).toInstant()
        }

        val spec = ExpenseEntity::accountId.equal(accountResolver.accountId) and
                ExpenseEntity::createdDate.equal(instantDate)
        expensesRepository.findAll(spec)
    }

    // FIXME: convert result to DTO
    fun postExpenses(dtos: Collection<ExpenseDto>): Iterable<ExpenseEntity> =
        locks.retryWithLock(accountResolver.accountId) {
            dtos
                .validate(POST_VALIDATORS)
                .map { create(it) }
                .let { expensesRepository.saveAll(it) }
        }

    // FIXME: convert result to DTO
    fun putExpenses(dtos: Collection<ExpenseDto>): Iterable<ExpenseEntity> =
        locks.retryWithLock(accountResolver.accountId) {
            dtos
                .validate(PUT_VALIDATORS)
                .map { update(it) }
                .let { expensesRepository.saveAll(it) }
        }

    private fun create(dto: ExpenseDto): ExpenseEntity {
        val isUniqueDescription = findOneByAccountIdAndDescription(accountResolver.accountId, dto.description) == null
        val description = dto.description + if (isUniqueDescription) "" else postfix()
        val serverVersion = INITIAL_SERVER_VERSION + if (isUniqueDescription) 0 else 1

        return dto.toExpenseEntity(
            accountResolver.accountId,
            accountResolver.userId,
            serverVersion,
            Utils.firstDayOfMonth(),
            description
        ).apply {
            spendings += dto.spendings.toSpendingEntities(this, createdBy)
        }
    }

    private fun update(dto: ExpenseDto): ExpenseEntity {
        val entity = expensesRepository.findFirstByAccountIdAndUuid(accountResolver.accountId, dto.uuid)
            ?: throw NotFoundException("Expense uuid == ${dto.uuid} is not found")

        if (dto.clientVersion <= entity.serverVersion) {
            throw ConflictException(entity)
        }

        with(entity) {
            description = dto.description
            category = dto.category
            amount = dto.amount
            closedDate = dto.closedDate
            deleted = dto.deleted
            serverVersion = dto.clientVersion
            modifiedBy = accountResolver.userId
            modifiedDate = Instant.now()
            spendings.updateAllFrom(dto.spendings.toSpendingEntities(this, accountResolver.userId))
        }

        return entity
    }

    private fun postfix() = " (${accountResolver.userId}-${String.format("%x", Random.nextInt(1000, 9999))})"

    private fun Iterable<SpendingDto>.toSpendingEntities(owner: ExpenseEntity, createdBy: String, deleted: Boolean = false) =
        map { spending -> spending.toSpendingEntity(owner, createdBy, deleted) }

    private fun findOneByAccountIdAndDescription(
        accountId: String,
        description: String
    ) = expensesRepository.findOne(
        ExpenseEntity::accountId.equal(accountId) and
                ExpenseEntity::description.equal(description) and
                ExpenseEntity::serverVersion.equal(INITIAL_SERVER_VERSION) and
                ExpenseEntity::deleted.equal(false)
    ).orElse(null)

    private fun Collection<ExpenseDto>.validate(validators: List<Validator>): Collection<ExpenseDto> {
        validators.onEach { it.validate(this) }
        return this
    }

    internal companion object {
        const val INITIAL_SERVER_VERSION = 1L

        val DATE_FORMAT = SimpleDateFormat("yyyy/MM/dd")

        private val PUT_VALIDATORS = listOf(PayloadLengthValidator, UuidsValidator)
        private val POST_VALIDATORS = listOf(PayloadLengthValidator, DescriptionsValidator, UuidsValidator)
    }
}