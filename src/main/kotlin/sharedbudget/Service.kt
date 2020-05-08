package sharedbudget

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sharedbudget.entities.ExpenseEntity
import sharedbudget.entities.ExpensesRepository
import sharedbudget.entities.InputExpenseDto
import sharedbudget.entities.OutputExpenseDto
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
    private val locks: Locks,
    private val mapper: ObjectMapper
) {

    @Transactional(readOnly = true)
    fun getExpenses(date: String? = null): Iterable<OutputExpenseDto> = locks.retryWithLock(accountResolver.accountId) {

        val instantDate = when (date) {
            null -> Instant.now()
            else -> DATE_FORMAT.parse(date).toInstant()
        }

        val spec = ExpenseEntity::accountId.equal(accountResolver.accountId) and
                ExpenseEntity::createdDate.equal(instantDate)
        expensesRepository.findAll(spec).map { it.toOutputExpenseDto() }
    }

    fun postExpenses(dtos: Collection<InputExpenseDto>): Iterable<OutputExpenseDto> =
        locks.retryWithLock(accountResolver.accountId) {
            dtos
                .validate(POST_VALIDATORS)
                .map { create(it) }
                .let { expensesRepository.saveAll(it) }
                .map { it.toOutputExpenseDto() }
        }

    fun putExpenses(dtos: Collection<InputExpenseDto>): Iterable<OutputExpenseDto> =
        locks.retryWithLock(accountResolver.accountId) {
            dtos
                .validate(PUT_VALIDATORS)
                .map { update(it) }
                .let { expensesRepository.saveAll(it) }
                .map { it.toOutputExpenseDto() }
        }

    private fun create(dto: InputExpenseDto): ExpenseEntity {
        val isUniqueDescription = findOneByAccountIdAndDescriptionAndCreatedDate(
            accountResolver.accountId,
            dto.description,
            dto.createdDate
        ) == null
        val description = dto.description + if (isUniqueDescription) "" else postfix()
        val serverVersion = dto.clientVersion + if (isUniqueDescription) 0 else 1

        return dto.toExpenseEntity(
            accountResolver.accountId,
            accountResolver.userId,
            description,
            serverVersion
        ).apply {
            spendings += dto.spendings.toSpendingEntities(this, createdBy)
        }
    }

    private fun update(dto: InputExpenseDto): ExpenseEntity {
        val entity = expensesRepository.findFirstByAccountIdAndUuid(accountResolver.accountId, dto.uuid)
            ?: throw NotFoundException("Expense uuid == ${dto.uuid} is not found")

        if (dto.clientVersion <= entity.serverVersion) {
            throw ConflictException(mapper.writeValueAsString(entity.toOutputExpenseDto()))
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

    private fun Iterable<SpendingDto>.toSpendingEntities(
        owner: ExpenseEntity,
        createdBy: String
    ) =
        map { spending -> spending.toSpendingEntity(owner, createdBy) }

    private fun findOneByAccountIdAndDescriptionAndCreatedDate(
        accountId: String,
        description: String,
        createdDate: Instant
    ) = expensesRepository.findOne(
        ExpenseEntity::accountId.equal(accountId) and
                ExpenseEntity::description.equal(description) and
                ExpenseEntity::createdDate.equal(createdDate)
    ).orElse(null)

    private fun Collection<InputExpenseDto>.validate(validators: List<Validator>): Collection<InputExpenseDto> {
        validators.onEach { it.validate(this) }
        return this
    }

    internal companion object {
        val DATE_FORMAT = SimpleDateFormat("yyyy/MM/dd")

        private val PUT_VALIDATORS = listOf(PayloadLengthValidator, UuidsValidator)
        private val POST_VALIDATORS =
            listOf(PayloadLengthValidator, DescriptionsValidator, UuidsValidator, NullModifiedDateValidators)
    }
}