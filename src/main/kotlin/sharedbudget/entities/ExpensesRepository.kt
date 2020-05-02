package sharedbudget.entities

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import sharedbudget.entities.ExpenseEntity

interface ExpensesRepository : JpaRepository<ExpenseEntity, String>, JpaSpecificationExecutor<ExpenseEntity>