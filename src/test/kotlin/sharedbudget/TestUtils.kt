package sharedbudget

import com.github.javafaker.Faker

object TestUtils {
    private val faker = Faker()

    fun generateExpenseDto(description: String = faker.food().fruit()) = expenseDto {
        this.description = description
        this.category = faker.commerce().productName()
        +spendingDto {
            comment = faker.hitchhikersGuideToTheGalaxy().quote()
        }
        +spendingDto {
            comment = faker.hitchhikersGuideToTheGalaxy().quote()
        }
    }
}