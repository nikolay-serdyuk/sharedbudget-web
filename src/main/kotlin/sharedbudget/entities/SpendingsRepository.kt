package sharedbudget.entities

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import sharedbudget.entities.SpendingEntity

interface SpendingsRepository : JpaRepository<SpendingEntity, String>, JpaSpecificationExecutor<SpendingEntity>