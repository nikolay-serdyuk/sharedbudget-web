package sharedbudget

import com.github.javafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import sharedbudget.entities.ExpensesRepository
import sharedbudget.entities.SpendingsRepository

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class RepositoryTest @Autowired constructor(
    private val expensesRepository: ExpensesRepository,
    private val spendingsRepository: SpendingsRepository
) {

    @BeforeEach
    fun beforeEach() {
        spendingsRepository.deleteAll()
        expensesRepository.deleteAll()
        spendingsRepository.flush()
        expensesRepository.flush()
    }

    @Test
    fun `violate EXPENSES_POSITIVE_AMOUNT constraint`() {
        val accountId = Faker().name().firstName()

        val expenseDto1 = expenseDto {
            amount = 0
        }
        assertThatThrownBy {
            expenseDto1.toExpenseEntity(accountId, accountId, expenseDto1.description, INITIAL_CLIENT_VERSION)
                .let { expensesRepository.save(it) }
        }
            .isInstanceOf(DataIntegrityViolationException::class.java)
            .hasMessageContaining("expenses_positive_amount")

        val expenseDto2 = expenseDto {
            amount = -1
        }
        assertThatThrownBy {
            expenseDto2.toExpenseEntity(accountId, accountId, expenseDto2.description, INITIAL_CLIENT_VERSION)
                .let { expensesRepository.save(it) }
        }
            .isInstanceOf(DataIntegrityViolationException::class.java)
            .hasMessageContaining("expenses_positive_amount")
    }

    @Test
    fun `violate SPENDINGS_POSITIVE_AMOUNT constraint`() {
        val accountId = Faker().name().firstName()

        val expenseDto1 = expenseDto {
            +spendingDto {
                amount = 0
            }
        }
        assertThatThrownBy {
            expenseDto1.toExpenseEntity(accountId, accountId, expenseDto1.description, INITIAL_CLIENT_VERSION)
                .apply { spendings += expenseDto1.spendings.map { it.toSpendingEntity(this, accountId) } }
                .let { expensesRepository.save(it) }
        }
            .isInstanceOf(DataIntegrityViolationException::class.java)
            .hasMessageContaining("spendings_positive_amount")

        val expenseDto2 = expenseDto {
            +spendingDto {
                amount = -1
            }
        }
        assertThatThrownBy {
            expenseDto2.toExpenseEntity(accountId, accountId, expenseDto2.description, INITIAL_CLIENT_VERSION)
                .apply { spendings += expenseDto1.spendings.map { it.toSpendingEntity(this, accountId) } }
                .let { expensesRepository.save(it) }
        }
            .isInstanceOf(DataIntegrityViolationException::class.java)
            .hasMessageContaining("spendings_positive_amount")
    }

    @Test
    fun `violate EXPENSES_NOT_BLANK_DESCRIPTION constraint`() {
        val accountId = Faker().name().firstName()

        val expenseDto = expenseDto {
            description = ""
        }
        assertThatThrownBy {
            expenseDto.toExpenseEntity(accountId, accountId, expenseDto.description, INITIAL_CLIENT_VERSION)
                .let { expensesRepository.save(it) }
        }
            .isInstanceOf(DataIntegrityViolationException::class.java)
            .hasMessageContaining("expenses_not_blank_description")
    }

    @Test
    fun `violate EXPENSES_NOT_BLANK_CATEGORY constraint`() {
        val accountId = Faker().name().firstName()

        val expenseDto = expenseDto {
            category = ""
        }
        assertThatThrownBy {
            expenseDto.toExpenseEntity(accountId, accountId, expenseDto.description, INITIAL_CLIENT_VERSION)
                .let { expensesRepository.save(it) }
        }
            .isInstanceOf(DataIntegrityViolationException::class.java)
            .hasMessageContaining("expenses_not_blank_category")
    }
}