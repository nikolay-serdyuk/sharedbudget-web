package sharedbudget.entities

import java.util.*

data class CategoryEntity(
    val accountId: Long,
    val uuid: UUID,
    val label: String
)
