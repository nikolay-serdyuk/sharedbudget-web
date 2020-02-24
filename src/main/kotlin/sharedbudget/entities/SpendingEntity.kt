package sharedbudget.entities

import java.time.Instant
import java.util.*

class SpendingEntity(
    val accountId: Long,
    val uuid: UUID,
    val amount: Long,
    val comment: String? = null,
    val expenseUuid: UUID,
    var deleted: Boolean,
    val createdBy: String,
    val createdDate: Instant
)
