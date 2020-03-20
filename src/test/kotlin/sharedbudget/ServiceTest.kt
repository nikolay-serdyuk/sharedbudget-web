package sharedbudget

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import sharedbudget.entities.ExpenseDto
import sharedbudget.entities.ExpenseEntity

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
        val assert = ExpenseEntityAssert.assertThat(this)
            .hasUuid(expenseDto.uuid)
            .hasDescription(expenseDto.description)
            .hasCategory(expenseDto.category)
            .hasAmount(expenseDto.amount)
            .hasDeleted(expenseDto.deleted)

        expenseDto.spendings
            .associateBy { it.uuid }
            .forEach { (key, spendingDto) ->
                assert.getSpending(key) {
                    this.hasUuid(spendingDto.uuid)
                        .hasAmount(spendingDto.amount)
                        .hasComment(spendingDto.comment)
                        .hasDeleted(spendingDto.deleted)
                }
            }
    }

    private fun generateExpenseDto() = expenseDto {
        spendings = mutableSetOf(
            spendingDto {},
            spendingDto {}
        )
    }
}