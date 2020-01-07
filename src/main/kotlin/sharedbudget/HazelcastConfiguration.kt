package sharedbudget

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.config.Config
import com.hazelcast.config.GlobalSerializerConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HazelcastConfiguration {
    @Bean
    fun config(objectMapper: ObjectMapper) = Config().apply {

        with(networkConfig.join) {
            multicastConfig.isEnabled = false
            tcpIpConfig.isEnabled = true
            tcpIpConfig.members = listOf("localhost")
        }

        serializationConfig.globalSerializerConfig = GlobalSerializerConfig()
            .setImplementation(HazelcastGlobalSerializer(objectMapper))
    }
}