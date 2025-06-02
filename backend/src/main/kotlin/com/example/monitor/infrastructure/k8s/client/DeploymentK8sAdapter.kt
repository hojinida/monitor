package com.example.monitor.infrastructure.k8s.client

import com.example.monitor.domain.error.CustomException
import io.fabric8.kubernetes.api.model.apps.Deployment as K8sDeployment
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import org.springframework.stereotype.Component

@Component
class DeploymentK8sAdapter(
    private val kubernetesClient: KubernetesClient,
    private val k8sImageCheck: K8sImageCheck
) {

    fun updateReplicas(namespace: String, name: String, replicas: Int) {
        val deployment = getDeployment(namespace, name)
        val patchedDeployment = DeploymentBuilder(deployment).editSpec().withReplicas(replicas).endSpec().build()

        applyPatch(namespace, name, patchedDeployment)
    }

    fun updateImageTag(namespace: String, name: String, newTag: String?) {
        val deployment = getDeployment(namespace, name)

        val oldImage = deployment.spec?.template?.spec?.containers?.firstOrNull()?.image
            ?: throw CustomException("이미지 정보를 찾을 수 없습니다.")

        var imageName = oldImage.substringBefore(":")
        if (newTag != "") {
            imageName = "$imageName:$newTag"
        }

        k8sImageCheck.validateImage(namespace, imageName)

        val patchedDeployment =
            DeploymentBuilder(deployment).editSpec().editTemplate().editSpec().editContainer(0).withImage(imageName)
                .endContainer().endSpec().endTemplate().endSpec().build()

        applyPatch(namespace, name, patchedDeployment)
    }

    private fun getDeployment(namespace: String, name: String): K8sDeployment {
        return kubernetesClient.apps().deployments().inNamespace(namespace).withName(name).get()
            ?: throw CustomException("Deployment를 찾을 수 없습니다.")
    }

    private fun applyPatch(namespace: String, name: String, patchedDeployment: K8sDeployment) {
        kubernetesClient.apps().deployments().inNamespace(namespace).withName(name).patch(patchedDeployment)
    }
}
