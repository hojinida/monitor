package com.example.monitor.infrastructure.persistence

import com.example.monitor.domain.namespace.NameSpace

interface NameSpaceRepository {
    fun findAll(): List<NameSpace>
    fun findByName(name: String): NameSpace?
    fun save(nameSpace: NameSpace)
    fun deleteByName(name: String)
}
