package sharedbudget

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sharedbudget.entities.ExpenseEntity
import sharedbudget.entities.ExpenseDto
import sharedbudget.entities.ExpensesRepository
import sharedbudget.entities.SpendingDto
import java.lang.Long.max
import java.text.SimpleDateFormat
import java.time.Instant

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

    fun postExpenses(dtos: Collection<ExpenseDto>): Iterable<ExpenseEntity> =
        locks.retryWithLock(accountResolver.accountId) {
            dtos
                .validate(POST_VALIDATORS)
                .flatMap { mergeOrCreate(it) }
                .let { expensesRepository.saveAll(it) }
        }

    private fun mergeOrCreate(dto: ExpenseDto): Iterable<ExpenseEntity> {
        var entity = findOneByAccountIdAndDescription(accountResolver.accountId, dto.description)
        val isNew = entity == null

        if (isNew) {
            entity = dto.toExpenseEntity(
                accountResolver.accountId,
                accountResolver.userId,
                INITIAL_SERVER_VERSION,
                Utils.firstDayOfMonth()
            )
        } else {
            // FIXME: return a conflict or implement a conflict resolver?
            entity.amount = max(entity.amount, dto.amount)
        }
        entity.spendings += dto.spendings.toSpendingEntities(entity)

        if (isNew) {
            return listOf(entity)
        }

        val deletedEntity = dto.toExpenseEntity(
            accountResolver.accountId,
            accountResolver.userId,
            INITIAL_SERVER_VERSION,
            Utils.firstDayOfMonth()
        ).also {
            it.deleted = true
            it.spendings += dto.spendings.toSpendingEntities(entity, deleted = true)
        }

        return listOf(deletedEntity, entity)
    }

    private fun Iterable<SpendingDto>.toSpendingEntities(owner: ExpenseEntity, deleted: Boolean = false) =
        map { spending -> spending.toSpendingEntity(owner, deleted) }

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

        private val POST_VALIDATORS = listOf(PayloadLengthValidator, DescriptionsValidator, UuidsValidator)
    }
}