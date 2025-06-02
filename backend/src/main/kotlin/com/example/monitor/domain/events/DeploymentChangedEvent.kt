package com.example.monitor.domain.events

import com.example.monitor.domain.util.DeploymentStatusUtil
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.client.Watcher.Action
import io.fabric8.kubernetes.api.model.apps.Deployment as K8sDeployment

data class DeploymentChangedEvent(
    val action: String,
    val name: String,
    val namespace: String,
    val status: String,
    val imageName: String,
    val imageTag: String,
    val availableReplicas: Int,
    val desiredReplicas: Int,
) {
    companion object {
        fun fromK8s(action: Action, resource: K8sDeployment): DeploymentChangedEvent {
            val name = resource.metadata.name
            val namespace = resource.metadata.namespace

            val desired = resource.spec?.replicas ?: 0
            val available = resource.status?.availableReplicas ?: 0

            val finalStatus = DeploymentStatusUtil.getDeploymentStatus(resource)

            val image = resource.spec?.template?.spec?.containers?.firstOrNull()?.image ?: "unknown"

            val imageName = image.substringBefore(":")
            val imageTag = image.substringAfter(":", "")

            return DeploymentChangedEvent(
                action = action.name,
                name = name,
                namespace = namespace,
                status = finalStatus.name,
                imageName = imageName,
                imageTag = imageTag,
                availableReplicas = available,
                desiredReplicas = desired
            )
        }
    }
}
