package sharedbudget

import sharedbudget.entities.InputExpenseDto
import javax.ws.rs.BadRequestException

interface Validator {
    fun validate(dtos: Collection<InputExpenseDto>)
}

object PayloadLengthValidator : Validator {

    override fun validate(dtos: Collection<InputExpenseDto>) {
        if (dtos.size > EXPENSES_LIMIT) {
            throw BadRequestException("Request contains too many expenses (limit is $EXPENSES_LIMIT)")
        }
        dtos.forEach {
            if (it.spendings.size > SPENDINGS_LIMIT) {
                throw BadRequestException("Expense uuid == ${it.uuid} contains too many spendings (limit is $SPENDINGS_LIMIT)")
            }
        }
    }

    private const val EXPENSES_LIMIT = 100
    private const val SPENDINGS_LIMIT = 50
}

object NullModifiedDateValidators : Validator {

    override fun validate(dtos: Collection<InputExpenseDto>) {
        for (dto in dtos) {
            if (dto.modifiedDate != null) {
                throw BadRequestException("Expense uuid == ${dto.uuid} contains non-null modifiedDate")
            }
        }
    }
}

object DescriptionsValidator : Validator {

    override fun validate(dtos: Collection<InputExpenseDto>) {
        val descriptions = mutableSetOf<String>()
        for (dto in dtos) {
            if (!descriptions.add(dto.description)) {
                throw BadRequestException("found multiple expenses with same description == ${dto.description}")
            }
        }
    }
}

object UuidsValidator : Validator {

    override fun validate(dtos: Collection<InputExpenseDto>) {
        val expenseUuids = mutableSetOf<String>()
        val spendingsUuids = mutableSetOf<String>()
        for (dto in dtos) {
            if (!expenseUuids.add(dto.uuid)) {
                throw BadRequestException("found multiple expenses with same uuid == ${dto.uuid}")
            }
            for (spending in dto.spendings) {
                if (!spendingsUuids.add(spending.uuid)) {
                    throw BadRequestException("found multiple spendings with same uuid == ${dto.uuid}")
                }
            }
        }
    }
}
