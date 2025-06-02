package com.example.monitor.infrastructure.k8s.watch

import com.example.monitor.application.sse.SseEventPublisher
import com.example.monitor.domain.events.NameSpaceChangedEvent
import com.example.monitor.domain.namespace.NameSpace
import com.example.monitor.domain.namespace.repository.InMemoryNameSpaceRepository
import io.fabric8.kubernetes.api.model.Namespace as K8sNamespace
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.Watcher.Action
import io.fabric8.kubernetes.client.WatcherException
import org.springframework.stereotype.Component

@Component
class NameSpaceWatchAdapter(
    private val inMemoryNameSpaceRepository: InMemoryNameSpaceRepository,
    private val sseEventPublisher: SseEventPublisher,
    kubernetesClient: KubernetesClient
) : AbstractKubernetesWatchAdapter<K8sNamespace>(kubernetesClient) {

    override fun startWatching(client: KubernetesClient): AutoCloseable {
        return client.namespaces().watch(object : Watcher<K8sNamespace> {
            override fun eventReceived(action: Action, resource: K8sNamespace) {
                handleEvent(action, resource)
            }

            override fun onClose(cause: WatcherException?) {
                this@NameSpaceWatchAdapter.onWatchClosed(cause)
            }
        })
    }

    private fun handleEvent(action: Action, resource: K8sNamespace) {
        when (action) {
            Action.ADDED, Action.MODIFIED -> {
                inMemoryNameSpaceRepository.save(NameSpace.fromK8s(resource))
                sseEventPublisher.onNameSpaceChanged(NameSpaceChangedEvent.fromK8s(action, resource))
            }

            Action.DELETED -> {
                inMemoryNameSpaceRepository.deleteByName(resource.metadata.name)
                sseEventPublisher.onNameSpaceChanged(NameSpaceChangedEvent.fromK8s(action, resource))
            }

            Action.ERROR, Action.BOOKMARK -> {
            }
        }
    }
}
