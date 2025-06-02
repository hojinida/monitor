package com.example.monitor.application.sse

import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CopyOnWriteArrayList

@Service
class SseService {

    private val emitters = CopyOnWriteArrayList<SseEmitter>()

    fun createEmitter(): SseEmitter {
        val emitter = SseEmitter(Long.MAX_VALUE)
        emitters += emitter

        emitter.onCompletion { emitters -= emitter }
        emitter.onTimeout { emitters -= emitter }
        emitter.onError { emitters -= emitter }

        return emitter
    }

    fun broadcast(eventName: String, eventData: Any) {
        val deadEmitters = mutableListOf<SseEmitter>()
        val json = ObjectMapperSingleton.mapper.writeValueAsString(eventData)

        emitters.forEach { emitter ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .name(eventName)
                        .data(json)
                )
            } catch (ex: Exception) {
                deadEmitters += emitter
            }
        }

        emitters.removeAll(deadEmitters)
    }
}

object ObjectMapperSingleton {
    val mapper = com.fasterxml.jackson.databind.ObjectMapper()
}
