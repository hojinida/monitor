package com.example.monitor.infrastructure.k8s.watch

import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.WatcherException
import jakarta.annotation.PostConstruct
import org.springframework.scheduling.annotation.Scheduled

abstract class AbstractKubernetesWatchAdapter<T>(
    private val kubernetesClient: KubernetesClient
) {
    protected var watchHandle: AutoCloseable? = null

    @Volatile
    protected var needRewatch: Boolean = true

    @PostConstruct
    fun onStartup() {
        tryRewatch()
    }

    @Scheduled(fixedDelay = 5000)
    fun scheduledRewatch() {
        if (needRewatch) {
            tryRewatch()
        }
    }

    private fun tryRewatch() {
        watchHandle?.close()

        try {
            watchHandle = startWatching(kubernetesClient)
            needRewatch = false
        } catch (ex: Exception) {
            needRewatch = true
        }
    }

    protected fun onWatchClosed(cause: WatcherException?) {
        needRewatch = true
    }

    protected abstract fun startWatching(client: KubernetesClient): AutoCloseable
}
