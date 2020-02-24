package sharedbudget.entities

import java.time.Instant
import java.util.*

data class ExpenseEntity(
    val accountId: Long,
    val uuid: UUID,
    val description: String,
    val category: String,
    val amount: Long,
    val closedDate: Date? = null,
    val spendings: MutableSet<SpendingEntity>,
    val delete: Boolean,
    // FIXME: add @Version
    val serverVersion: Long,
    // FIXME: add @CreatedBy
    val createdBy: String,
    // FIXME: add @CreatedDate
    val createdDate: Instant,
    // FIXME: add @LastModifiedBy
    val modifiedBy: String? = null,
    // FIXME: add @LastModifiedDate
    val modifiedDate: Instant? = null
)
