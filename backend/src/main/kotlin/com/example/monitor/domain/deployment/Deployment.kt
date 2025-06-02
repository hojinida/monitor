package com.example.monitor.domain.deployment

import com.example.monitor.domain.util.DeploymentStatusUtil
import io.fabric8.kubernetes.api.model.apps.Deployment as K8sDeployment

class Deployment(
    val name: String,
    val namespace: String,
    val status: DeploymentStatus,
    val availableReplicas: Int,
    val desiredReplicas: Int,
    val image: Image,
) {
    companion object {
        fun fromK8s(k8sDep: K8sDeployment): Deployment {
            val name = k8sDep.metadata.name
            val ns = k8sDep.metadata.namespace
            val desired = k8sDep.spec?.replicas ?: 0
            val available = k8sDep.status?.availableReplicas ?: 0

            val status = DeploymentStatusUtil.getDeploymentStatus(k8sDep)

            return Deployment(
                name = name,
                namespace = ns,
                status = status,
                availableReplicas = available,
                desiredReplicas = desired,
                image = Image.Companion.fromK8s(k8sDep)
            )
        }
    }
}
