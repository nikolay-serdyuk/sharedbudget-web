package sharedbudget

import org.springframework.stereotype.Component

@Component
class AccountResolver {
    val accountId: String
        get() = "TestAccount"

    val userId: String
        get() = "TestUser"
}