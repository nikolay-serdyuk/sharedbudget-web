package sharedbudget

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import sharedbudget.entities.ExpenseDto
import sharedbudget.entities.ExpenseEntity
import sharedbudget.entities.SpendingDto
import sharedbudget.entities.SpendingEntity
import kotlin.random.Random
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ServiceTest @Autowired constructor(
    private val service: Service, private val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun beforeEach() {
        jdbcTemplate.update("DELETE FROM SPENDINGS")
        jdbcTemplate.update("DELETE FROM EXPENSES")
    }

    @Test
    fun `check simple post`() {
        val inputExpense1 = generateExpenseDto()
        val inputExpense2 = generateExpenseDto()

        val outputExpenseMap = service.postExpenses(listOf(inputExpense1, inputExpense2)).associateBy { it.uuid }

        val outputExpense1 = outputExpenseMap.getValue(inputExpense1.uuid)
        outputExpense1.assertIsEqualTo(inputExpense1)
        val outputExpense2 = outputExpenseMap.getValue(inputExpense2.uuid)
        outputExpense2.assertIsEqualTo(inputExpense2)
    }

    private fun ExpenseEntity.assertIsEqualTo(expenseDto: ExpenseDto) {
        assertThat(uuid).isEqualTo(expenseDto.uuid)
        assertThat(description).isEqualTo(expenseDto.description)
        assertThat(category).isEqualTo(expenseDto.category)
        assertThat(amount).isEqualTo(expenseDto.amount)
        assertThat(deleted).isEqualTo(expenseDto.deleted)
        assertThat(spendings.size).isEqualTo(expenseDto.spendings.size)
        val spendingMap = expenseDto.spendings.associateBy { it.uuid }
        spendings.forEach() { it.assertIsEqualTo(spendingMap.getValue(it.uuid)) }
    }

    private fun SpendingEntity.assertIsEqualTo(spendingDto: SpendingDto) {
        assertThat(uuid).isEqualTo(spendingDto.uuid)
        assertThat(amount).isEqualTo(spendingDto.amount)
        assertThat(comment).isEqualTo(spendingDto.comment)
        assertThat(deleted).isEqualTo(spendingDto.deleted)
    }

    private fun generateExpenseDto() = ExpenseDto(
        uuid = randomString(),
        description = randomString(),
        category = randomString(),
        amount = randomLong(MIN_EXPENSE_AMOUNT, MAX_EXPENSE_AMOUNT),
        spendings = mutableSetOf(),
        deleted = false
    ).apply {
        for (i in 1..2) {
            spendings.add(generateSpendingDto())
        }
    }

    private fun generateSpendingDto() = SpendingDto(
        uuid = randomString(),
        amount = randomLong(MIN_SPENDING_AMOUNT, MAX_SPENDING_AMOUNT),
        comment = randomString(),
        deleted = false
    )

    private fun randomString() = UUID.randomUUID().toString()
    private fun randomLong(from: Long, until: Long) = Random.nextLong(from = from, until = until)

    private companion object {
        const val MIN_EXPENSE_AMOUNT = 1000L
        const val MAX_EXPENSE_AMOUNT = 10 * MIN_EXPENSE_AMOUNT
        const val MIN_SPENDING_AMOUNT = 1L
        const val MAX_SPENDING_AMOUNT = MIN_EXPENSE_AMOUNT / 10
    }
}