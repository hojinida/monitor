package com.example.monitor.application.namespace

import com.example.monitor.application.namespace.dto.NameSpaceResponse
import com.example.monitor.infrastructure.persistence.NameSpaceRepository
import org.springframework.stereotype.Service

@Service
class NameSpaceQueryService(
    private val namespaceRepository: NameSpaceRepository
) {
    fun getAllNameSpaces(): List<NameSpaceResponse> {
        val namespaces = namespaceRepository.findAll()
        return namespaces.map { NameSpaceResponse.fromDomain(it) }
    }
}
