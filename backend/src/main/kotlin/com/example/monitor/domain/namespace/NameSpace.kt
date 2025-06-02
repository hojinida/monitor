package com.example.monitor.domain.namespace

import com.fasterxml.jackson.annotation.JsonFormat
import io.fabric8.kubernetes.api.model.Namespace as K8sNamespace
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

data class NameSpace(
    val name: String, var status: NameSpaceStatus, @JsonFormat(
        shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX"
    )
    var creationTimestamp: OffsetDateTime
) {

    companion object {
        fun fromK8s(k8sNs: K8sNamespace): NameSpace {
            val name = k8sNs.metadata.name
            val phase = k8sNs.status?.phase ?: "Unknown"
            val status = when {
                phase.equals("Active", ignoreCase = true) -> NameSpaceStatus.ACTIVE
                phase.equals("Terminating", ignoreCase = true) -> NameSpaceStatus.TERMINATING
                else -> NameSpaceStatus.UNKNOWN
            }
            val createdAt = k8sNs.metadata.creationTimestamp
            val formattedCreatedAt = createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            return NameSpace(name, status, OffsetDateTime.parse(formattedCreatedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        }
    }
}

