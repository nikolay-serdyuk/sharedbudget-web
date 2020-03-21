package sharedbudget

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles

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

    private fun generateExpenseDto() = expenseDto {
        +spendingDto {}
        +spendingDto {}
    }
}