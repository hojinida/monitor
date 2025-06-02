package com.example.monitor.application.deployment.dto

import com.example.monitor.domain.deployment.Deployment

data class DeploymentResponse(
    val name: String,
    val namespace: String,
    val status: String,
    val availableReplicas: Int,
    val desiredReplicas: Int,
    val imageName: String,
    val imageTag: String,
) {
    companion object {
        fun fromDomain(dep: Deployment): DeploymentResponse {
            return DeploymentResponse(
                name = dep.name,
                namespace = dep.namespace,
                status = dep.status.name,
                availableReplicas = dep.availableReplicas,
                desiredReplicas = dep.desiredReplicas,
                imageName = dep.image.name,
                imageTag = dep.image.tag,
            )
        }
    }
}
