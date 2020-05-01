package sharedbudget

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import com.github.javafaker.Faker
import sharedbudget.Service.Companion.INITIAL_SERVER_VERSION
import sharedbudget.TestUtils.generateExpenseDto
import sharedbudget.entities.ExpenseEntity
import java.lang.Long.max


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ServiceTest @Autowired constructor(
    private val service: Service,
    private val expensesRepository: ExpensesRepository,
    private val spendingsRepository: SpendingsRepository
) {
    private val faker = Faker()

    @BeforeEach
    fun beforeEach() {
        spendingsRepository.deleteAll()
        expensesRepository.deleteAll()
        spendingsRepository.flush()
        expensesRepository.flush()
    }

    @Test
    fun `check simple post`() {
        val inputExpense1 = generateExpenseDto()
        val inputExpense2 = generateExpenseDto()

        val outputExpenseMap = service.postExpenses(listOf(inputExpense1, inputExpense2)).associateBy { it.uuid }

        val outputExpense1 = outputExpenseMap.getValue(inputExpense1.uuid)
        outputExpense1.assertEqualTo(inputExpense1)
        val outputExpense2 = outputExpenseMap.getValue(inputExpense2.uuid)
        outputExpense2.assertEqualTo(inputExpense2)

        ExpenseEntityAssert.assertThat(outputExpense1)
            .hasVersion(INITIAL_SERVER_VERSION)
        // TODO: add more checks for auditable fields
    }

    @Test
    fun `three people post expense with same description`() {
        val description = faker.hobbit().quote()
        val inputExpense1 = generateExpenseDto(description = description)
        val inputExpense2 = generateExpenseDto(description = description)
        val inputExpense3 = generateExpenseDto(description = description)

        val outputExpense1 = service.postExpenses(listOf(inputExpense1)).single()
        val outputExpense2 = service.postExpenses(listOf(inputExpense2)).single()
        assert(outputExpense2.spendings.size == 4)
        outputExpense2.assertUpdatedFrom(outputExpense1)

        val outputExpense3 = service.postExpenses(listOf(inputExpense3)).single()
        assert(outputExpense3.spendings.size == 6)
        outputExpense3.assertUpdatedFrom(outputExpense2)

        assert(expensesRepository.findAll().size == 1)
    }


    private fun ExpenseEntity.assertUpdatedFrom(other: ExpenseEntity) {
        val otherSpendingsMap = other.spendings.associateBy { it.uuid }.toMutableMap()

        ExpenseEntityAssert.assertThat(this)
            .hasUuid(other.uuid)
            .hasDescription(other.description)
            .hasCategory(other.category)
            .hasAmount(max(amount, other.amount))
            .hasDeleted(other.deleted)
            .onEachSpending { (uuid, spendingEntityAssert) ->
                val otherSpending = otherSpendingsMap[uuid]
                if (otherSpending != null) {
                    spendingEntityAssert.hasUuid(otherSpending.uuid)
                    spendingEntityAssert.hasAmount(otherSpending.amount)
                    spendingEntityAssert.hasComment(otherSpending.comment)
                    spendingEntityAssert.hasDeleted(otherSpending.deleted)
                    otherSpendingsMap.remove(uuid)
                }
            }
        assert(otherSpendingsMap.isEmpty())
    }
}