package sharedbudget

import com.github.javafaker.Faker
import org.springframework.stereotype.Component

@Component
class AccountResolver {
    var accountId: String = Faker().name().firstName()
    var userId: String = Faker().funnyName().name()
}
