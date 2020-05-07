package sharedbudget

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import sharedbudget.entities.ExpenseEntity
import javax.ws.rs.ClientErrorException
import javax.ws.rs.InternalServerErrorException
import javax.ws.rs.core.Response

class UnableAcquireLockException(accountId: String) :
    InternalServerErrorException("Can't acquire Lock[$accountId]")

class UnableReleaseLockException(accountId: String) :
    InternalServerErrorException("Can't release Lock[$accountId]")

class ConflictException(private val entity: ExpenseEntity) : ClientErrorException(Response.Status.CONFLICT) {
    override fun toString(): String = mapper.writeValueAsString(entity)
}

private val mapper = jacksonObjectMapper()