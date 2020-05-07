package sharedbudget.entities

import java.time.Instant
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.OneToMany
import javax.persistence.Table

sealed class Expense<T : Spending> {
    abstract val uuid: String
    abstract val description: String
    abstract val category: String
    abstract val amount: Long
    abstract val closedDate: Instant?
    abstract val spendings: MutableSet<T>
    abstract val deleted: Boolean
}

data class ExpenseDto(
    override val uuid: String,
    override val description: String,
    override val category: String,
    override val amount: Long,
    override val closedDate: Instant?,
    override val spendings: MutableSet<SpendingDto>,
    override val deleted: Boolean,
    val clientVersion: Long
) : Expense<SpendingDto>() {

    fun toExpenseEntity(
        accountId: String,
        userId: String,
        serverVersion: Long,
        createdDate: Instant,
        description: String = this.description
    ) =
        ExpenseEntity(
            accountId = accountId,
            uuid = uuid,
            description = description,
            category = category,
            amount = amount,
            closedDate = null,
            spendings = mutableSetOf(),
            deleted = deleted,
            serverVersion = serverVersion,
            createdBy = userId,
            createdDate = createdDate,
            modifiedBy = null,
            modifiedDate = null
        )
}

@Entity
@Table(name = "EXPENSES")
@IdClass(AccountUuidPk::class)
class ExpenseEntity(

    @Id
    val accountId: String,

    @Id
    override val uuid: String,

    override var description: String,

    override var category: String,

    override var amount: Long,

    override var closedDate: Instant?,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "owner", fetch = FetchType.EAGER)
    override val spendings: MutableSet<SpendingEntity> = mutableSetOf(),

    override var deleted: Boolean,

    var serverVersion: Long,

    // FIXME: add @CreatedBy
    val createdBy: String,

    // FIXME: add @CreatedDate
    val createdDate: Instant,

    // FIXME: add @LastModifiedBy
    var modifiedBy: String?,

    // FIXME: add @LastModifiedDate
    var modifiedDate: Instant?

) : Expense<SpendingEntity>()