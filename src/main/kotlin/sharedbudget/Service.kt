package sharedbudget

import org.springframework.stereotype.Service
import sharedbudget.entities.ExpenseEntity
import sharedbudget.entities.ExpenseDto
import java.text.SimpleDateFormat
import java.time.Instant

@Service
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

    fun postExpenses(expenses: Iterable<ExpenseDto>): Iterable<ExpenseEntity> {
        val outputExpenses = expenses.map {
            ExpenseEntity(
                accountId = accountResolver.accountId,
                uuid = it.uuid,
                description = it.description,
                category = it.category,
                amount = it.amount,
                closedDate = null,
                spendings = mutableSetOf(),
                deleted = false,
                serverVersion = INITIAL_SERVER_VERSION,
                createdBy = accountResolver.userId,
                createdDate = Utils.firstDayOfMonth(),
                modifiedBy = null,
                modifiedDate = null
            ).apply {
                spendings.addAll(it.spendings.map { spending -> spending.toSpendingEntity(this) })
            }
        }

        return expensesRepository.saveAll(outputExpenses)
    }

    private companion object {
        val DATE_FORMAT = SimpleDateFormat("yyyy/MM/dd")
        const val INITIAL_SERVER_VERSION = 1L
    }
}