package sharedbudget

import org.springframework.retry.RetryPolicy
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy
import org.springframework.retry.policy.NeverRetryPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import com.hazelcast.core.HazelcastInstance
import org.springframework.stereotype.Component

@Component
class Locks(private val hazelcastInstance: HazelcastInstance) {
    private val subscriberLockCache
        get() = hazelcastInstance.getMap<String, Boolean>(ACCOUNT_LOCKS_MAP_NAME)

    private val policyMap: Map<Class<out Throwable>, RetryPolicy> = HashMap<Class<out Throwable>, RetryPolicy>()
        .also {
            it[UnableAcquireLockException::class.java] = SimpleRetryPolicy(NUM_RETRIES)
            it[Exception::class.java] = NeverRetryPolicy()
        }

    private val retryTemplate = RetryTemplate().apply {
        setRetryPolicy(ExceptionClassifierRetryPolicy().apply { setPolicyMap(policyMap) })
        setBackOffPolicy(ExponentialRandomBackOffPolicy().apply {
            initialInterval = INITIAL_INTERVAL
            maxInterval = MAX_INTERVAL
        })
    }

    fun <T : Any> retryWithLock(accountId: String, block: () -> T): T =
        retryTemplate.execute<T, Exception> { withLock(accountId, block) }

    internal fun <T : Any> withLock(accountId: String, block: () -> T?): T? {
        lock(accountId)
        try {
            return block()
        } finally {
            unlock(accountId)
        }
    }

    internal fun lock(accountId: String) {
        subscriberLockCache.putIfAbsent(accountId, false)
        if (!subscriberLockCache.replace(accountId, false, true)) {
            throw UnableAcquireLockException(accountId)
        }
    }

    internal fun unlock(accountId: String) {
        if (!subscriberLockCache.replace(accountId, true, false)) {
            throw UnableReleaseLockException(accountId)
        }
    }

    internal fun isLocked(accountId: String) = subscriberLockCache[accountId]

    companion object {
        const val ACCOUNT_LOCKS_MAP_NAME = "ACCOUNT_LOCKS_MAP_NAME"
        private const val NUM_RETRIES = 3
        private const val INITIAL_INTERVAL = 500L
        private const val MAX_INTERVAL = 10000L
    }
}
