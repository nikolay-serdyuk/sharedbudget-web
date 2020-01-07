package sharedbudget.entities

import javax.persistence.*

import java.time.Instant
import java.util.*

data class ExpenseEntity(
    val accountId: Long,
    val uuid: UUID,
    val category: String,
    val description: String,
    val amount: Long,
    val closedDate: Date? = null,
    @Version
    val serverVersion: Long,
    val createdBy: String,
    val createdDate: Instant,
    val modifiedBy: String? = null,
    val modifiedDate: Instant? = null,
    val spendings: MutableSet<SpendingEntity>,
    val deletedAfterMerge: Boolean
)
