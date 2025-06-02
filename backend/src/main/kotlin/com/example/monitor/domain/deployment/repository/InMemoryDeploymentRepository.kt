package com.example.monitor.domain.deployment.repository

import com.example.monitor.domain.deployment.Deployment
import com.example.monitor.infrastructure.persistence.DeploymentRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryDeploymentRepository : DeploymentRepository {
    private val storage = ConcurrentHashMap<String, ConcurrentHashMap<String, Deployment>>()

    override fun findAllByNameSpace(namespace: String): List<Deployment> =
        storage[namespace]?.values?.toList() ?: emptyList()

    override fun findByNameSpaceAndName(namespace: String, name: String): Deployment? = storage[namespace]?.get(name)

    override fun save(deployment: Deployment) {
        val subMap = storage.computeIfAbsent(deployment.namespace) { ConcurrentHashMap() }
        subMap[deployment.name] = deployment
    }

    override fun delete(namespace: String, name: String) {
        storage[namespace]?.remove(name)
    }
}
