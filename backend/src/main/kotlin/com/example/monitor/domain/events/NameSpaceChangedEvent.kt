package com.example.monitor.domain.events

import com.example.monitor.application.util.AgeCalculator
import io.fabric8.kubernetes.api.model.Namespace as K8sNamespace
import io.fabric8.kubernetes.client.Watcher.Action


data class NameSpaceChangedEvent(
    val action: String, val name: String, val status: String, val age: String
) {
    companion object {
        fun fromK8s(action: Action, resource: K8sNamespace): NameSpaceChangedEvent {
            val phase = resource.status?.phase?.lowercase()
            val status = if (phase == "active") "Active" else "Terminating"

            val age = AgeCalculator.computeAgeString(resource.metadata.creationTimestamp)

            return NameSpaceChangedEvent(
                action = action.name, name = resource.metadata.name, status = status, age = age
            )
        }
    }
}

