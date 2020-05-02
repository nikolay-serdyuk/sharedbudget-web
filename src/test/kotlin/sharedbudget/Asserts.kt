package sharedbudget

import org.assertj.core.api.AbstractObjectAssert
import sharedbudget.entities.ExpenseEntity
import sharedbudget.entities.ExpenseInterface
import sharedbudget.entities.SpendingEntity
import java.time.Instant
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
    fun onEachSpending(block: (Map.Entry<String, SpendingEntityAssert>) -> Unit) = apply {
        actual.spendings.associateBy({ it.uuid }, { SpendingEntityAssert.assertThat(it) }).onEach { block(it) }
    }

    fun hasVersion(serverVersion: Long) =
        apply { hasFieldOrPropertyWithValue(ExpenseEntity::serverVersion, serverVersion) }

    fun hasCreatedBy(createdBy: String) = apply { hasFieldOrPropertyWithValue(ExpenseEntity::createdBy, createdBy) }
    fun hasCreatedDate(createdDate: Instant) =
        apply { hasFieldOrPropertyWithValue(ExpenseEntity::createdDate, createdDate) }

    fun hasModifiedBy(modifiedBy: String?) =
        apply { hasFieldOrPropertyWithValue(ExpenseEntity::modifiedBy, modifiedBy) }

    fun hasModifiedDate(modifiedDate: Instant) =
        apply { hasFieldOrPropertyWithValue(ExpenseEntity::modifiedDate, modifiedDate) }

    fun hasClosedDate(closedDate: Instant) =
        apply { hasFieldOrPropertyWithValue(ExpenseEntity::closedDate, closedDate) }

    companion object {
        fun assertThat(expenseEntity: ExpenseEntity) = ExpenseEntityAssert(expenseEntity)
    }
}

fun ExpenseEntity.assertEqualTo(other: ExpenseInterface<*>) {
    assert(spendings.size == other.spendings.size)

    val otherSpendingsMap = other.spendings
        .associateBy { it.uuid }

    ExpenseEntityAssert.assertThat(this)
        .hasUuid(other.uuid)
        .hasDescription(other.description)
        .hasCategory(other.category)
        .hasAmount(other.amount)
        .hasDeleted(other.deleted)
        .onEachSpending { (uuid, spendingEntityAssert) ->
            val otherSpending = otherSpendingsMap.getValue(uuid)
            spendingEntityAssert.hasUuid(otherSpending.uuid)
                .hasAmount(otherSpending.amount)
                .hasComment(otherSpending.comment)
                .hasDeleted(otherSpending.deleted)
        }
}

private fun AbstractObjectAssert<*, *>.hasFieldOrPropertyWithValue(property: KProperty1<*, *>, value: Any?) =
    hasFieldOrPropertyWithValue(property.name, value)

