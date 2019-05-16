package sharedbudget.entities

import java.util.*

data class DescriptionEntity(
    val accountId: Long,
    val uuid: UUID,
    val label: String
)
