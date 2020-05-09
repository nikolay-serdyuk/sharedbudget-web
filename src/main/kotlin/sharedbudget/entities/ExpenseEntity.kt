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
    abstract val spendings: Set<T>
    abstract val deleted: Boolean
    abstract val createdDate: Instant
    abstract val modifiedDate: Instant?
}

data class InputExpenseDto(
    override val uuid: String,
    override val description: String,
    override val category: String,
    override val amount: Long,
    override val closedDate: Instant?,
    override val spendings: Set<SpendingDto>,
    override val deleted: Boolean,
    override val createdDate: Instant,
    override val modifiedDate: Instant?,
    val clientVersion: Long
) : Expense<SpendingDto>() {

    fun toExpenseEntity(
        accountId: String,
        userId: String,
        description: String = this.description,
        serverVersion: Long
    ) = ExpenseEntity(
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
        modifiedDate = modifiedDate
    )
}

data class OutputExpenseDto(
    override val uuid: String,
    override val description: String,
    override val category: String,
    override val amount: Long,
    override val closedDate: Instant?,
    override val spendings: Set<SpendingDto>,
    override val deleted: Boolean,
    override val createdDate: Instant,
    override val modifiedDate: Instant?,
    val serverVersion: Long
) : Expense<SpendingDto>()

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

    override val createdDate: Instant,

    // FIXME: add @LastModifiedBy
    var modifiedBy: String?,

    override var modifiedDate: Instant?

) : Expense<SpendingEntity>() {

    fun toOutputExpenseDto() = OutputExpenseDto(
        uuid = uuid,
        description = description,
        category = category,
        amount = amount,
        closedDate = closedDate,
        spendings = spendings.map { it.toSpendingDto() }.toSet(),
        deleted = deleted,
        createdDate = createdDate,
        modifiedDate = modifiedDate,
        serverVersion = serverVersion
    )
}