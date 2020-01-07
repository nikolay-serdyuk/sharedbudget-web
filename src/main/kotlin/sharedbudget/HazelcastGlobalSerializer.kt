package sharedbudget

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.nio.ObjectDataInput
import com.hazelcast.nio.ObjectDataOutput
import com.hazelcast.nio.serialization.StreamSerializer
import com.hazelcast.util.ExceptionUtil

class HazelcastGlobalSerializer(private val objectMapper: ObjectMapper) : StreamSerializer<Any> {
    override fun getTypeId() = 42

    override fun destroy() {
        // no need
    }

    override fun write(out: ObjectDataOutput, `object`: Any) {
        out.writeUTF(`object`.javaClass.name)
        out.writeByteArray(objectMapper.writeValueAsBytes(`object`))
    }

    override fun read(`in`: ObjectDataInput): Any {
        val clazz = `in`.readUTF()
        return try {
            objectMapper.readValue(`in`.readByteArray(), Class.forName(clazz))
        } catch (e: ClassNotFoundException) {
            throw ExceptionUtil.peel(e)
        }
    }
}
