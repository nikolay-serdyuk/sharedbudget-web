package sharedbudget

import sharedbudget.entities.ExpenseDto
import sharedbudget.entities.SpendingDto
import java.util.*
import kotlin.random.Random

class SpendingDtoBuilder {
    var uuid: String = randomString()
    var amount: Long = randomLong(MIN_SPENDING_AMOUNT, MAX_SPENDING_AMOUNT)
    var comment: String? = randomString()
    var deleted: Boolean = false

    fun build() = SpendingDto(
        uuid = uuid,
        amount = amount,
        comment = comment,
        deleted = deleted
    )
}

fun spendingDto(init: SpendingDtoBuilder.() -> Unit): SpendingDto {
    val builder = SpendingDtoBuilder()
    builder.init()
    return builder.build()
}

class ExpenseDtoBuilder {
    var uuid: String = randomString()
    var description: String = randomString()
    var category: String = randomString()
    var amount: Long = randomLong(MIN_EXPENSE_AMOUNT, MAX_EXPENSE_AMOUNT)
    private var spendings: MutableSet<SpendingDto> = mutableSetOf()
    var deleted: Boolean = false

    operator fun SpendingDto.unaryPlus() {
        spendings.add(this)
    }

    fun build() = ExpenseDto(
        uuid = uuid,
        description = description,
        category = category,
        amount = amount,
        spendings = spendings,
        deleted = deleted
    )
}

fun expenseDto(init: ExpenseDtoBuilder.() -> Unit): ExpenseDto {
    val builder = ExpenseDtoBuilder()
    builder.init()
    return builder.build()
}

fun randomString() = UUID.randomUUID().toString()
fun randomLong(from: Long, until: Long) = Random.nextLong(from = from, until = until)

private const val MIN_EXPENSE_AMOUNT = 1000L
private const val MAX_EXPENSE_AMOUNT = 10 * MIN_EXPENSE_AMOUNT
private const val MIN_SPENDING_AMOUNT = 1L
private const val MAX_SPENDING_AMOUNT = MIN_EXPENSE_AMOUNT / 10
