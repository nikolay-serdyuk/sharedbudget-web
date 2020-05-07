package sharedbudget.entities

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface ExpensesRepository : JpaRepository<ExpenseEntity, String>, JpaSpecificationExecutor<ExpenseEntity> {
    fun findFirstByAccountIdAndUuid(accountId: String, uuid: String): ExpenseEntity?
}