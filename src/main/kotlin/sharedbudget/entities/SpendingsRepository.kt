package sharedbudget.entities

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface SpendingsRepository : JpaRepository<SpendingEntity, String>, JpaSpecificationExecutor<SpendingEntity>