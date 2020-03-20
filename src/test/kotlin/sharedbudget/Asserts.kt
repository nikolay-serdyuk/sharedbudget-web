package sharedbudget

import org.assertj.core.api.AbstractObjectAssert
import sharedbudget.entities.ExpenseEntity
import sharedbudget.entities.SpendingEntity
import kotlin.reflect.KProperty1

class SpendingEntityAssert(spendingEntity: SpendingEntity) :
    AbstractObjectAssert<SpendingEntityAssert, SpendingEntity>(spendingEntity, SpendingEntityAssert::class.java) {

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
    fun getSpending(uuid: String, block: SpendingEntityAssert.() -> Unit) = apply {
        block(SpendingEntityAssert.assertThat(actual.spendings.associateBy { it.uuid }.getValue(uuid)))
    }

    companion object {
        fun assertThat(expenseEntity: ExpenseEntity) = ExpenseEntityAssert(expenseEntity)
    }
}

private fun AbstractObjectAssert<*, *>.hasFieldOrPropertyWithValue(property: KProperty1<*, *>, value: Any?) =
    hasFieldOrPropertyWithValue(property.name, value)

