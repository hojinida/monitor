package com.example.monitor.infrastructure.k8s.client

import com.example.monitor.domain.error.CustomException
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import org.springframework.stereotype.Component

@Component
class K8sImageCheck(
    private val kubernetesClient: KubernetesClient
) {

    fun validateImage(namespace: String, imageName: String): Boolean {
        val podName = "test-pod-${System.currentTimeMillis()}"

        val pod = PodBuilder()
            .withNewMetadata()
            .withName(podName)
            .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName("test-container")
            .withImage(imageName)
            .withCommand("echo", "Checking image...")
            .endContainer()
            .withRestartPolicy("Never")
            .endSpec()
            .build()

        kubernetesClient.pods().inNamespace(namespace).resource(pod).create()

        return waitForPodStatus(namespace, podName)
    }

    private fun waitForPodStatus(namespace: String, podName: String): Boolean {
        try {
            repeat(10) {
                val pod = kubernetesClient.pods().inNamespace(namespace).withName(podName).get()
                pod?.status?.containerStatuses?.firstOrNull()?.state?.waiting?.let { waitingState ->
                    if (waitingState.reason == "ErrImagePull" || waitingState.reason == "ImagePullBackOff") {
                        throw CustomException("이미지의 태그가 잘못되었습니다.")
                    }
                }
                if (pod?.status?.phase == "Succeeded") {
                    return true
                }
                Thread.sleep(1000)
            }
            throw CustomException("이미지를 불러오는 중 오류가 발생했습니다.)")
        } finally {
            cleanupPod(namespace, podName)
        }
    }

    private fun cleanupPod(namespace: String, podName: String) {
        kubernetesClient.pods().inNamespace(namespace).withName(podName).delete()
    }
}

