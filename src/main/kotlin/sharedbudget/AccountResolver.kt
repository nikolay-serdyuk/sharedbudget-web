package sharedbudget

import com.github.javafaker.Faker
import org.springframework.stereotype.Component

@Component
class AccountResolver {
    val accountId: String = Faker().name().firstName()
    val userId: String = Faker().funnyName().name()
}
