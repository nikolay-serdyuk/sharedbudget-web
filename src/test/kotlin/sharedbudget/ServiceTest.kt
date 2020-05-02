package sharedbudget

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import com.github.javafaker.Faker
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import sharedbudget.Service.Companion.INITIAL_SERVER_VERSION
import sharedbudget.TestUtils.generateExpenseDto
import sharedbudget.entities.ExpenseEntity
import sharedbudget.entities.ExpensesRepository
import sharedbudget.entities.SpendingsRepository
import java.lang.Long.max
import java.util.*
import javax.ws.rs.BadRequestException


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ServiceTest @Autowired constructor(
    private val service: Service,
    private val locks: Locks,
    private val accountResolver: AccountResolver,
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
        val inputExpense1 = generateExpenseDto(description = "abc")
        val inputExpense2 = generateExpenseDto(description = "xyz")

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
    fun `three people post expenses with same description`() {
        val description = faker.food().fruit()
        val inputExpense1 = generateExpenseDto(description = description)
        val inputExpense2 = generateExpenseDto(description = description)
        val inputExpense3 = generateExpenseDto(description = description)

        val outputExpense1 = service.postExpenses(listOf(inputExpense1)).single()
        val outputExpenses2 = service.postExpenses(listOf(inputExpense2)).associateBy { it.uuid }
        val outputExpense2 = outputExpenses2.getValue(outputExpense1.uuid)
        assert(outputExpense2.spendings.size == 4)
        assert(outputExpense2.amount >= outputExpense1.amount)
        outputExpense2.assertUpdatedFrom(outputExpense1)

        val outputExpenses3 = service.postExpenses(listOf(inputExpense3)).associateBy { it.uuid }
        val outputExpense3 = outputExpenses3.getValue(outputExpense2.uuid)
        assert(outputExpense3.spendings.size == 6)
        assert(outputExpense3.amount >= outputExpense2.amount)
        outputExpense3.assertUpdatedFrom(outputExpense2)

        assert(expensesRepository.findAll().size == 3)
    }

    @Test
    fun `check thrown exception if payload size is too big`() {
        val expensesDtos = IntRange(1, 1000).map { generateExpenseDto() }
        assertThatThrownBy { service.postExpenses(expensesDtos) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageStartingWith("Request contains too many expenses")

        val expensesDto = expenseDto {
            spendings = IntRange(1, 1000).map { spendingDto {} }.toMutableSet()
        }
        assertThatThrownBy { service.postExpenses(listOf(expensesDto)) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("contains too many spendings")
    }

    @Test
    fun `check thrown exception if payload contains multiple expenses with similar description`() {
        val description = faker.food().fruit()
        val inputExpense1 = generateExpenseDto(description = description)
        val inputExpense2 = generateExpenseDto(description = description)

        assertThatThrownBy { service.postExpenses(listOf(inputExpense1, inputExpense2)) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageStartingWith("found multiple expenses with same description")
    }

    @Test
    fun `check thrown exception if payload contains multiple expenses with similar uuid`() {
        val uuid = UUID.randomUUID().toString()
        val inputExpense1 = expenseDto {
            this.uuid = uuid
        }
        val inputExpense2 = expenseDto {
            this.uuid = uuid
        }

        assertThatThrownBy { service.postExpenses(listOf(inputExpense1, inputExpense2)) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageStartingWith("found multiple expenses with same uuid")
    }

    @Test
    fun `check thrown exception if payload contains multiple spendings with similar uuid`() {
        val uuid = UUID.randomUUID().toString()
        val inputExpense = expenseDto {
            +spendingDto {
                this.uuid = uuid
            }
            +spendingDto {
                this.uuid = uuid
            }
        }

        assertThatThrownBy { service.postExpenses(listOf(inputExpense)) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageStartingWith("found multiple spendings with same uuid")
    }

    @Test
    fun `try to post for a locked account`() {
        locks.lock(accountResolver.accountId)
        val inputExpense = generateExpenseDto()

        assertThatThrownBy { service.postExpenses(listOf(inputExpense)) }
            .isInstanceOf(Locks.UnableAcquireLockException::class.java)

        locks.unlock(accountResolver.accountId)
    }

    @Test
    fun `try to post when another account is locked`() {
        val accountId = randomString()
        locks.lock(accountId)
        val inputExpense = generateExpenseDto()
        assertDoesNotThrow { service.postExpenses(listOf(inputExpense)) }
        locks.unlock(accountId)
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