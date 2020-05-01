package sharedbudget

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sharedbudget.entities.ExpenseEntity
import sharedbudget.entities.ExpenseDto
import java.lang.Long.max
import java.text.SimpleDateFormat
import java.time.Instant

@Service
@Transactional
class Service(private val accountResolver: AccountResolver, private val expensesRepository: ExpensesRepository) {

    fun getExpenses(date: String? = null): Iterable<ExpenseEntity> {

        val instantDate = when (date) {
            null -> Instant.now()
            else -> DATE_FORMAT.parse(date).toInstant()
        }

        val spec = ExpenseEntity::accountId.equal(accountResolver.accountId) and
                ExpenseEntity::createdDate.equal(instantDate)
        return expensesRepository.findAll(spec)
    }

    fun postExpenses(expenseDtos: Iterable<ExpenseDto>): Iterable<ExpenseEntity> {

        val expenseEntities = expenseDtos.map {
            findExistingOneOrCreate(it).apply {
                amount = max(amount, it.amount)
                spendings.addAll(it.spendings.map { spending -> spending.toSpendingEntity(this) })
            }
        }

        return expensesRepository.saveAll(expenseEntities)
    }

    private fun findExistingOneOrCreate(expenseDto: ExpenseDto) =
        findOneByAccountIdAndDescription(accountResolver.accountId, expenseDto.description)
            ?: expenseDto.toExpenseEntity(
                accountResolver.accountId,
                accountResolver.userId,
                INITIAL_SERVER_VERSION,
                Utils.firstDayOfMonth()
            )

    private fun findOneByAccountIdAndDescription(
        accountId: String,
        description: String
    ) = expensesRepository.findOne(
        ExpenseEntity::accountId.equal(accountId) and
                ExpenseEntity::description.equal(description) and
                ExpenseEntity::serverVersion.equal(INITIAL_SERVER_VERSION)
    ).orElse(null)

    internal companion object {
        val DATE_FORMAT = SimpleDateFormat("yyyy/MM/dd")
        const val INITIAL_SERVER_VERSION = 1L
    }
}