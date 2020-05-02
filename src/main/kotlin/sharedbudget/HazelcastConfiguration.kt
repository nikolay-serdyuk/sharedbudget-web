package sharedbudget

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.config.Config
import com.hazelcast.config.GlobalSerializerConfig
import com.hazelcast.config.ListenerConfig
import com.hazelcast.config.MapConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import sharedbudget.Locks.Companion.ACCOUNT_LOCKS_MAP_NAME

@Configuration
class HazelcastConfiguration {
    @Bean
    fun config(objectMapper: ObjectMapper) = Config().apply {

        with(networkConfig.join) {
            multicastConfig.isEnabled = false
            tcpIpConfig.isEnabled = true
            tcpIpConfig.members = listOf("localhost")
        }
        addListenerConfig(ListenerConfig()).addMapConfig(mapConfig())

        serializationConfig.globalSerializerConfig = GlobalSerializerConfig()
            .setImplementation(HazelcastGlobalSerializer(objectMapper))
    }

    private fun mapConfig() = MapConfig(ACCOUNT_LOCKS_MAP_NAME)
        .apply { timeToLiveSeconds = 3600 / 2 }
}