package sharedbudget.entities

import java.time.Instant
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.OneToMany
import javax.persistence.Table

interface ExpenseInterface<T : SpendingInterface> {
    val uuid: String
    val description: String
    val category: String
    val amount: Long
    val spendings: MutableSet<T>
    val deleted: Boolean
}

data class ExpenseDto(
    override val uuid: String,
    override val description: String,
    override val category: String,
    override val amount: Long,
    override val spendings: MutableSet<SpendingDto>,
    override val deleted: Boolean
) : ExpenseInterface<SpendingDto> {

    fun toExpenseEntity(accountId: String, userId: String, serverVersion: Long, createdDate: Instant) =
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

    override val description: String,

    override var category: String,

    override var amount: Long,

    var closedDate: Instant?,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "owner", fetch = FetchType.EAGER)
    override val spendings: MutableSet<SpendingEntity> = mutableSetOf(),

    override var deleted: Boolean,

    // FIXME: add @Version
    val serverVersion: Long,

    // FIXME: add @CreatedBy
    val createdBy: String,

    // FIXME: add @CreatedDate
    val createdDate: Instant,

    // FIXME: add @LastModifiedBy
    val modifiedBy: String?,

    // FIXME: add @LastModifiedDate
    val modifiedDate: Instant?

) : ExpenseInterface<SpendingEntity>