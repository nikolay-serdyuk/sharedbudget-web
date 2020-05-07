package sharedbudget

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import com.github.javafaker.Faker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import sharedbudget.Service.Companion.INITIAL_SERVER_VERSION
import sharedbudget.TestUtils.generateExpenseDto
import sharedbudget.entities.ExpensesRepository
import sharedbudget.entities.SpendingsRepository
import java.time.Instant
import java.util.*
import javax.ws.rs.BadRequestException
import javax.ws.rs.NotFoundException


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
    }

    @Test
    fun `three people post expenses with same description`() {
        val description = faker.food().fruit()
        val expenseDto1 = generateExpenseDto(description = description)
        val expenseDto2 = generateExpenseDto(description = description)
        val expenseDto3 = generateExpenseDto(description = description)

        val userId1 = Faker().funnyName().name()
        accountResolver.userId = userId1
        val expenseEntity1 = service.postExpenses(listOf(expenseDto1)).single()
        assertThat(expenseEntity1.description).isEqualTo(description)

        val userId2 = Faker().funnyName().name()
        accountResolver.userId = userId2
        val expenseEntity2 = service.postExpenses(listOf(expenseDto2)).single()
        assertThat(expenseEntity2.description).startsWith("$description ($userId2-")
        assertThat(expenseEntity2.serverVersion).isEqualTo(INITIAL_SERVER_VERSION + 1)

        val userId3 = Faker().funnyName().name()
        accountResolver.userId = userId3
        val expenseEntity3 = service.postExpenses(listOf(expenseDto3)).single()
        assertThat(expenseEntity3.description).startsWith("$description ($userId3-")
        assertThat(expenseEntity3.serverVersion).isEqualTo(INITIAL_SERVER_VERSION + 1)
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
        val expenseDto1 = generateExpenseDto(description = description)
        val expenseDto2 = generateExpenseDto(description = description)

        assertThatThrownBy { service.postExpenses(listOf(expenseDto1, expenseDto2)) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageStartingWith("found multiple expenses with same description")
    }

    @Test
    fun `check thrown exception if payload contains multiple expenses with similar uuid`() {
        val uuid = UUID.randomUUID().toString()
        val expenseDto1 = expenseDto {
            this.uuid = uuid
        }
        val expenseDto2 = expenseDto {
            this.uuid = uuid
        }

        assertThatThrownBy { service.postExpenses(listOf(expenseDto1, expenseDto2)) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageStartingWith("found multiple expenses with same uuid")
    }

    @Test
    fun `check thrown exception if payload contains multiple spendings with similar uuid`() {
        val uuid = UUID.randomUUID().toString()
        val expenseDto = expenseDto {
            +spendingDto {
                this.uuid = uuid
            }
            +spendingDto {
                this.uuid = uuid
            }
        }

        assertThatThrownBy { service.postExpenses(listOf(expenseDto)) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageStartingWith("found multiple spendings with same uuid")
    }

    @Test
    fun `check NotFoundException is thrown when updating unknown entity`() {
        val expenseDto = generateExpenseDto()

        assertThatThrownBy { service.putExpenses(listOf(expenseDto)) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `check ConflictException is thrown when patching an entity with old data`() {
        val uuid = UUID.randomUUID().toString()
        val initialExpenseDto = expenseDto {
            this.uuid = uuid
        }
        service.postExpenses(listOf(initialExpenseDto))

        val updatedExpenseDto = expenseDto {
            this.uuid = uuid
            this.clientVersion = INITIAL_SERVER_VERSION
        }
        assertThatThrownBy { service.putExpenses(listOf(updatedExpenseDto)) }
            .isInstanceOf(ConflictException::class.java)
    }

    @Test
    fun `check patch`() {
        val uuid = UUID.randomUUID().toString()
        val initialExpenseDto = expenseDto {
            this.uuid = uuid
            +spendingDto {  }
        }
        service.postExpenses(listOf(initialExpenseDto))

        val updatedExpenseDto = expenseDto {
            this.uuid = uuid
            closedDate = Instant.now()
            deleted = true
            clientVersion = INITIAL_SERVER_VERSION + 1
            +initialExpenseDto.spendings.single().copy(deleted = true)
            +spendingDto {  }
        }
        val updatedExpenseEntity = service.putExpenses(listOf(updatedExpenseDto)).single()
        updatedExpenseEntity.assertEqualTo(updatedExpenseDto)
    }

    @Test
    fun `try to post for a locked account`() {
        locks.lock(accountResolver.accountId)
        val expenseDto = generateExpenseDto()

        assertThatThrownBy { service.postExpenses(listOf(expenseDto)) }
            .isInstanceOf(UnableAcquireLockException::class.java)

        locks.unlock(accountResolver.accountId)
    }

    @Test
    fun `try to post when another account is locked`() {
        val accountId = randomString()
        locks.lock(accountId)
        val expenseDto = generateExpenseDto()
        assertDoesNotThrow { service.postExpenses(listOf(expenseDto)) }
        locks.unlock(accountId)
    }
}