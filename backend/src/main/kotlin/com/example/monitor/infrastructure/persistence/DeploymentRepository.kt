package com.example.monitor.infrastructure.persistence

import com.example.monitor.domain.deployment.Deployment

interface DeploymentRepository {
    fun findAllByNameSpace(namespace: String): List<Deployment>
    fun findByNameSpaceAndName(namespace: String, name: String): Deployment?
    fun save(deployment: Deployment)
    fun delete(namespace: String, name: String)
}
