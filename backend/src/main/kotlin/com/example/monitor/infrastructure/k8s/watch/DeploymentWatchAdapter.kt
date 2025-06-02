package com.example.monitor.infrastructure.k8s.watch

import com.example.monitor.application.sse.SseEventPublisher
import com.example.monitor.domain.deployment.Deployment
import com.example.monitor.domain.deployment.repository.InMemoryDeploymentRepository
import com.example.monitor.domain.events.DeploymentChangedEvent
import io.fabric8.kubernetes.api.model.apps.Deployment as K8sDeployment
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.Watcher.Action
import io.fabric8.kubernetes.client.WatcherException
import org.springframework.stereotype.Component

@Component
class DeploymentWatchAdapter(
    private val inMemoryDeploymentRepository: InMemoryDeploymentRepository,
    private val sseEventPublisher: SseEventPublisher,
    kubernetesClient: KubernetesClient,
) : AbstractKubernetesWatchAdapter<K8sDeployment>(kubernetesClient) {


    override fun startWatching(client: KubernetesClient): AutoCloseable {
        return client.apps().deployments().inAnyNamespace().watch(object : Watcher<K8sDeployment> {
            override fun eventReceived(action: Action, resource: K8sDeployment) {
                handleEvent(action, resource)
            }

            override fun onClose(cause: WatcherException?) {
                this@DeploymentWatchAdapter.onWatchClosed(cause)
            }
        })
    }

    private fun handleEvent(action: Action, resource: K8sDeployment) {
        when (action) {
            Action.ADDED, Action.MODIFIED -> {
                inMemoryDeploymentRepository.save(Deployment.fromK8s(resource))
                sseEventPublisher.onDeploymentChanged(
                    DeploymentChangedEvent.fromK8s(
                        action, resource
                    )
                )
            }

            Action.DELETED -> {
                inMemoryDeploymentRepository.delete(
                    resource.metadata.namespace, resource.metadata.name
                )
                sseEventPublisher.onDeploymentChanged(
                    DeploymentChangedEvent.fromK8s(
                        action, resource
                    )
                )
            }

            Action.ERROR -> {
                println("Error")
            }

            Action.BOOKMARK -> {
            }
        }
    }
}
