package sharedbudget

import org.assertj.core.api.AbstractObjectAssert
import sharedbudget.entities.ExpenseDto
import sharedbudget.entities.ExpenseEntity
import sharedbudget.entities.SpendingEntity
import kotlin.reflect.KProperty1

class SpendingEntityAssert(spendingEntity: SpendingEntity) :
    AbstractObjectAssert<SpendingEntityAssert, SpendingEntity>(spendingEntity, SpendingEntityAssert::class.java) {
    val uuid = spendingEntity.uuid

    fun hasUuid(uuid: String) = apply { hasFieldOrPropertyWithValue(SpendingEntity::uuid, uuid) }
    fun hasAmount(amount: Long) = apply { hasFieldOrPropertyWithValue(SpendingEntity::amount, amount) }
    fun hasComment(comment: String?) = apply { hasFieldOrPropertyWithValue(SpendingEntity::comment, comment) }
    fun hasDeleted(deleted: Boolean) = apply { hasFieldOrPropertyWithValue(SpendingEntity::deleted, deleted) }

    companion object {
        fun assertThat(spendingEntity: SpendingEntity) = SpendingEntityAssert(spendingEntity)
    }
}

class ExpenseEntityAssert(expenseEntity: ExpenseEntity) :
    AbstractObjectAssert<ExpenseEntityAssert, ExpenseEntity>(expenseEntity, ExpenseEntityAssert::class.java) {

    fun hasUuid(uuid: String) = apply { hasFieldOrPropertyWithValue(ExpenseEntity::uuid, uuid) }
    fun hasDescription(description: String) =
        apply { hasFieldOrPropertyWithValue(ExpenseEntity::description, description) }

    fun hasCategory(category: String) = apply { hasFieldOrPropertyWithValue(ExpenseEntity::category, category) }
    fun hasAmount(amount: Long) = apply { hasFieldOrPropertyWithValue(ExpenseEntity::amount, amount) }
    fun hasDeleted(deleted: Boolean) = apply { hasFieldOrPropertyWithValue(ExpenseEntity::deleted, deleted) }
    fun onEachSpending(block: (Map.Entry<String, SpendingEntityAssert>) -> Unit) = apply {
        actual.spendings.associateBy({ it.uuid }, { SpendingEntityAssert.assertThat(it) }).onEach { block(it) }
    }

    companion object {
        fun assertThat(expenseEntity: ExpenseEntity) = ExpenseEntityAssert(expenseEntity)
    }
}

fun ExpenseEntity.assertIsEqualTo(expenseDto: ExpenseDto) {
    val spendingsMap = expenseDto.spendings
        .associateBy { it.uuid }

    ExpenseEntityAssert.assertThat(this)
        .hasUuid(expenseDto.uuid)
        .hasDescription(expenseDto.description)
        .hasCategory(expenseDto.category)
        .hasAmount(expenseDto.amount)
        .hasDeleted(expenseDto.deleted)
        .onEachSpending { (uuid, spendingEntityAssert) ->
            val spendingDto = spendingsMap.getValue(uuid)
            spendingEntityAssert.hasUuid(spendingDto.uuid)
                .hasAmount(spendingDto.amount)
                .hasComment(spendingDto.comment)
                .hasDeleted(spendingDto.deleted)
        }
}

private fun AbstractObjectAssert<*, *>.hasFieldOrPropertyWithValue(property: KProperty1<*, *>, value: Any?) =
    hasFieldOrPropertyWithValue(property.name, value)

