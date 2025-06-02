package com.example.monitor.application.deployment

import com.example.monitor.application.deployment.dto.DeploymentResponse
import com.example.monitor.domain.error.CustomException
import com.example.monitor.infrastructure.k8s.client.DeploymentK8sAdapter
import com.example.monitor.infrastructure.persistence.DeploymentRepository
import org.springframework.stereotype.Service

@Service
class DeploymentService(
    private val deploymentRepository: DeploymentRepository,
    private val deploymentK8sAdapter: DeploymentK8sAdapter,
) {

    fun getDeploymentsInNamespace(ns: String): List<DeploymentResponse> =
        deploymentRepository.findAllByNameSpace(ns).map { DeploymentResponse.fromDomain(it) }

    fun updateReplicas(namespace: String, name: String, replicas: Int) {
        val existing = deploymentRepository.findByNameSpaceAndName(namespace, name)
            ?: throw CustomException("해당 Deployment가 존재하지 않습니다. (namespace=$namespace, name=$name)")

        require(replicas >= 0) { "Replicas 수는 양수이어야 합니다." }

        if (replicas == existing.availableReplicas) {
            throw CustomException("현재 Replicas 수와 동일합니다. (replicas=$replicas)")
        }

        deploymentK8sAdapter.updateReplicas(namespace, name, replicas)
    }

    fun updateImage(namespace: String, name: String, newImageTag: String?) {
        val existing = deploymentRepository.findByNameSpaceAndName(namespace, name)
            ?: throw CustomException("해당 Deployment가 존재하지 않습니다. (namespace=$namespace, name=$name)")


        if (newImageTag == existing.image.tag) {
            throw CustomException("현재 이미지 태그와 변경하려는 이미지 태그가 동일합니다. (tag=$newImageTag)")
        }

        deploymentK8sAdapter.updateImageTag(namespace, name, newImageTag)
    }
}
