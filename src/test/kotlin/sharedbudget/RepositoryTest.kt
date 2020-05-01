package sharedbudget

import com.github.javafaker.Faker
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import sharedbudget.TestUtils.generateExpenseDto
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class RepositoryTest @Autowired constructor(
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
    fun `violate UNIQUE_DESCRIPTION constraint`() {
        val description = faker.food().fruit()
        val accountId = Faker().name().firstName()
        val userId = Faker().funnyName().name()
        val date = Utils.firstDayOfMonth(Instant.now())
        generateExpenseDto(description = description)
            .toExpenseEntity(accountId, userId, Service.INITIAL_SERVER_VERSION, date)
            .let { expensesRepository.save(it) }
        Assertions.assertThrows(DataIntegrityViolationException::class.java) {
            generateExpenseDto(description = description)
                .toExpenseEntity(accountId, userId, Service.INITIAL_SERVER_VERSION, date)
                .let { expensesRepository.save(it) }
        }
    }

    @Test
    fun `violate EXPENSES_POSITIVE_AMOUNT constraint`() {
        val accountId = Faker().name().firstName()
        val userId = Faker().funnyName().name()
        val date = Utils.firstDayOfMonth(Instant.now())
        val expenseDto1 = expenseDto {
            amount = 0
        }
        Assertions.assertThrows(DataIntegrityViolationException::class.java) {
            expenseDto1.toExpenseEntity(accountId, userId, Service.INITIAL_SERVER_VERSION, date)
                .let { expensesRepository.save(it) }
        }
        val expenseDto2 = expenseDto {
            amount = -1
        }
        Assertions.assertThrows(DataIntegrityViolationException::class.java) {
            expenseDto2.toExpenseEntity(accountId, userId, Service.INITIAL_SERVER_VERSION, date)
                .let { expensesRepository.save(it) }
        }
    }

    @Test
    fun `violate SPENDINGS_POSITIVE_AMOUNT constraint`() {
        val accountId = Faker().name().firstName()
        val userId = Faker().funnyName().name()
        val date = Utils.firstDayOfMonth(Instant.now())
        val expenseDto1 = expenseDto {
            +spendingDto {
                amount = 0
            }
        }
        Assertions.assertThrows(DataIntegrityViolationException::class.java) {
            expenseDto1.toExpenseEntity(accountId, userId, Service.INITIAL_SERVER_VERSION, date)
                .apply { spendings.addAll(expenseDto1.spendings.map { it.toSpendingEntity(this) }) }
                .let { expensesRepository.save(it) }
        }
        val expenseDto2 = expenseDto {
            +spendingDto {
                amount = -1
            }
        }
        Assertions.assertThrows(DataIntegrityViolationException::class.java) {
            expenseDto2.toExpenseEntity(accountId, userId, Service.INITIAL_SERVER_VERSION, date)
                .apply { spendings.addAll(expenseDto1.spendings.map { it.toSpendingEntity(this) }) }
                .let { expensesRepository.save(it) }
        }
    }

    @Test
    fun `violate EXPENSES_NOT_BLANK_DESCRIPTION constraint`() {
        val accountId = Faker().name().firstName()
        val userId = Faker().funnyName().name()
        val date = Utils.firstDayOfMonth(Instant.now())
        val expenseDto = expenseDto {
            description = ""
        }
        Assertions.assertThrows(DataIntegrityViolationException::class.java) {
            expenseDto.toExpenseEntity(accountId, userId, Service.INITIAL_SERVER_VERSION, date)
                .let { expensesRepository.save(it) }
        }
    }

    @Test
    fun `violate EXPENSES_NOT_BLANK_CATEGORY constraint`() {
        val accountId = Faker().name().firstName()
        val userId = Faker().funnyName().name()
        val date = Utils.firstDayOfMonth(Instant.now())
        val expenseDto = expenseDto {
            category = ""
        }
        Assertions.assertThrows(DataIntegrityViolationException::class.java) {
            expenseDto.toExpenseEntity(accountId, userId, Service.INITIAL_SERVER_VERSION, date)
                .let { expensesRepository.save(it) }
        }
    }
}