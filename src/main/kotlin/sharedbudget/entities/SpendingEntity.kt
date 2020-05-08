package sharedbudget.entities

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

sealed class Spending {
    abstract val uuid: String
    abstract val amount: Long
    abstract val comment: String?
    abstract val deleted: Boolean
    abstract val createdDate: Instant
}

data class SpendingDto(
    override val uuid: String,
    override val amount: Long = 0,
    override val comment: String? = null,
    override val deleted: Boolean,
    override val createdDate: Instant
) : Spending() {

    fun toSpendingEntity(owner: ExpenseEntity, createdBy: String) = SpendingEntity(
        owner = owner,
        uuid = uuid,
        amount = amount,
        comment = comment,
        createdDate = createdDate,
        createdBy = createdBy,
        deleted = deleted
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

    override val createdDate: Instant

) : Spending(), UpdatableEntity<SpendingEntity> {

    @Id
    @Column(name = "ACCOUNT_ID")
    val accountId: String = owner.accountId

    @Column(name = "EXPENSE_UUID")
    val expenseUuid: String = owner.uuid

    override fun updateFrom(other: SpendingEntity): SpendingEntity {
        deleted = other.deleted
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (javaClass != other?.javaClass) return false

        val otherSpending = other as SpendingEntity

        return uuid == otherSpending.uuid
    }

    override fun hashCode(): Int = uuid.hashCode()

    fun toSpendingDto() = SpendingDto(
        uuid = uuid,
        amount = amount,
        comment = comment,
        deleted = deleted,
        createdDate = createdDate
    )
}