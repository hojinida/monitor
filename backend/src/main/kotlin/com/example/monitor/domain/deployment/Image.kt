package com.example.monitor.domain.deployment

import io.fabric8.kubernetes.api.model.apps.Deployment as K8sDeployment

data class Image(
    val name: String, val tag: String
) {

    companion object {
        fun fromK8s(k8sDep: K8sDeployment): Image {
            val container = k8sDep.spec.template.spec.containers[0]
            val image = container.image
            val name = image.substringBefore(":")
            val tag = image.substringAfter(":", "")
            return Image(name, tag)
        }
    }
}
