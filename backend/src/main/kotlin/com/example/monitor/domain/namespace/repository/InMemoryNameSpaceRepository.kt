package com.example.monitor.domain.namespace.repository

import com.example.monitor.domain.namespace.NameSpace
import com.example.monitor.infrastructure.persistence.NameSpaceRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

@Repository
class InMemoryNameSpaceRepository : NameSpaceRepository {

    private val storage = ConcurrentHashMap<String, NameSpace>()

    override fun findAll(): List<NameSpace> {
        return storage.values.toList()
    }

    override fun findByName(name: String): NameSpace? {
        return storage[name]
    }

    override fun save(nameSpace: NameSpace) {
        storage[nameSpace.name] = nameSpace
    }

    override fun deleteByName(name: String) {
        storage.remove(name)
    }
}
