package sharedbudget

import com.github.javafaker.Faker
import java.time.Instant

object TestUtils {
    private val faker = Faker()

    fun generateExpenseDto(description: String = faker.food().fruit(), createdDate: Instant = randomInstant()) = expenseDto {
        this.description = description
        this.category = faker.commerce().productName()
        this.createdDate = createdDate
        +spendingDto {
            comment = faker.hitchhikersGuideToTheGalaxy().quote()
        }
        +spendingDto {
            comment = faker.hitchhikersGuideToTheGalaxy().quote()
        }
    }
}