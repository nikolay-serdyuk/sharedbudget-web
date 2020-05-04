package sharedbudget

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LocksTest @Autowired constructor(private val locks: Locks) {

    @Test
    fun `check a simple lock`() {
        val accountId = randomString()

        locks.withLock(accountId) {
            assert(locks.isLocked(accountId) == true)
        }
        assert(locks.isLocked(accountId) == false)
    }

    @Test
    fun `try to unlock an unlocked lock`() {
        val accountId = randomString()

        locks.withLock(accountId) {
            assert(locks.isLocked(accountId) == true)
        }
        assert(locks.isLocked(accountId) == false)
        assertThatThrownBy {
            locks.unlock(accountId)
        }.isInstanceOf(Locks.UnableReleaseLockException::class.java)

        assertThatThrownBy {
            locks.unlock(randomString())
        }.isInstanceOf(Locks.UnableReleaseLockException::class.java)
    }

    @Test
    fun `check reentrant locking`() {
        val accountId = randomString()

        locks.withLock(accountId) {
            assertThatThrownBy {
                locks.withLock(accountId) {}
            }.isInstanceOf(Locks.UnableAcquireLockException::class.java)
        }
    }
}