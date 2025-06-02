package com.example.monitor.domain.util

import com.example.monitor.domain.deployment.DeploymentStatus
import io.fabric8.kubernetes.api.model.apps.Deployment as K8sDeployment

object DeploymentStatusUtil {
    fun getDeploymentStatus(dep: K8sDeployment): DeploymentStatus {
        val desired = dep.spec?.replicas ?: 0
        val available = dep.status?.availableReplicas ?: 0

        if (available >= desired) {
            return DeploymentStatus.RUNNING
        }

        if (0 < available && available < desired) {
            return DeploymentStatus.PENDING
        }

        return DeploymentStatus.FAILED
    }
}
