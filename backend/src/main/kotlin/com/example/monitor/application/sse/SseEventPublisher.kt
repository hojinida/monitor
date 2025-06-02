package com.example.monitor.application.sse

import com.example.monitor.domain.events.DeploymentChangedEvent
import com.example.monitor.domain.events.NameSpaceChangedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class SseEventPublisher(
    private val sseService: SseService
) {

    @EventListener
    fun onNameSpaceChanged(event: NameSpaceChangedEvent) {
        val data = mapOf(
            "type" to "NAMESPACE",
            "action" to event.action,
            "name" to event.name,
            "status" to event.status,
            "age" to event.age
        )
        sseService.broadcast("namespaceUpdate", data)
    }

    @EventListener
    fun onDeploymentChanged(event: DeploymentChangedEvent) {
        val data = mapOf(
            "type" to "DEPLOYMENT",
            "action" to event.action,
            "namespace" to event.namespace,
            "name" to event.name,
            "status" to event.status,
            "imageName" to event.imageName,
            "imageTag" to event.imageTag,
            "availableReplicas" to event.availableReplicas,
            "desiredReplicas" to event.desiredReplicas
        )
        sseService.broadcast("deploymentUpdate", data)
    }
}
