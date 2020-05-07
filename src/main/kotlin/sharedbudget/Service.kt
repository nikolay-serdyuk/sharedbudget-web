package sharedbudget

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sharedbudget.entities.ExpenseEntity
import sharedbudget.entities.ExpenseDto
import sharedbudget.entities.ExpensesRepository
import sharedbudget.entities.SpendingDto
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
                .map { create(it) }
                .let { expensesRepository.saveAll(it) }
        }

    private fun create(dto: ExpenseDto): ExpenseEntity {
        val isUniqueDescription = findOneByAccountIdAndDescription(accountResolver.accountId, dto.description) == null
        val description = dto.description + if (isUniqueDescription) "" else descriptionPostfix()

        return dto.toExpenseEntity(
            accountResolver.accountId,
            accountResolver.userId,
            INITIAL_SERVER_VERSION,
            Utils.firstDayOfMonth(),
            description
        ).apply {
            spendings += dto.spendings.toSpendingEntities(this)
        }
    }

    private fun descriptionPostfix() = " (${accountResolver.userId})"

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