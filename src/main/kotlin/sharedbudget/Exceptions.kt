package sharedbudget

import javax.ws.rs.ClientErrorException
import javax.ws.rs.InternalServerErrorException
import javax.ws.rs.core.Response

class UnableAcquireLockException(accountId: String) :
    InternalServerErrorException("Can't acquire Lock[$accountId]")

class UnableReleaseLockException(accountId: String) :
    InternalServerErrorException("Can't release Lock[$accountId]")

class ConflictException(serializedEntity: String) :
    ClientErrorException(serializedEntity, Response.Status.CONFLICT)
