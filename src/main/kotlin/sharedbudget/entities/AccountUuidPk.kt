package sharedbudget.entities

import java.io.Serializable

data class AccountUuidPk(
    val accountId: String? = null,
    val uuid: String? = null
) : Serializable {
    constructor() : this(null, null)
}
