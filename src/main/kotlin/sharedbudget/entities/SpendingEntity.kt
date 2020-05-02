package sharedbudget.entities

import sharedbudget.Utils
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.JoinColumn
import javax.persistence.JoinColumns
import javax.persistence.ManyToOne
import javax.persistence.Table

interface SpendingInterface {
    val uuid: String
    val amount: Long
    val comment: String?
    val deleted: Boolean
}

data class SpendingDto(
    override val uuid: String,
    override val amount: Long = 0,
    override val comment: String? = null,
    override val deleted: Boolean
) : SpendingInterface {

    fun toSpendingEntity(owner: ExpenseEntity, deleted: Boolean = false) = SpendingEntity(
        owner = owner,
        uuid = uuid,
        amount = amount,
        comment = comment,
        createdDate = Utils.firstDayOfMonth(),
        createdBy = owner.createdBy,
        deleted = this.deleted or deleted
    )
}

@Entity
@Table(name = "SPENDINGS")
@IdClass(AccountUuidPk::class)
class SpendingEntity(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns(
        JoinColumn(name = "ACCOUNT_ID", insertable = false, updatable = false),
        JoinColumn(name = "EXPENSE_UUID", insertable = false, updatable = false)
    )
    val owner: ExpenseEntity,

    @Id
    override val uuid: String,

    override val amount: Long,

    override val comment: String? = null,

    override var deleted: Boolean,

    val createdBy: String,

    val createdDate: Instant

) : SpendingInterface {

    @Id
    @Column(name = "ACCOUNT_ID")
    val accountId: String = owner.accountId

    @Column(name = "EXPENSE_UUID")
    val expenseUuid: String = owner.uuid
}