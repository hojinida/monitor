package com.example.monitor.presentation.controller

import com.example.monitor.application.namespace.NameSpaceQueryService
import com.example.monitor.application.namespace.dto.NameSpaceResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/namespaces")
class NameSpaceController(
    private val nameSpaceQueryService: NameSpaceQueryService
) {
    @GetMapping
    fun getAllNamespaces(): ResponseEntity<List<NameSpaceResponse>> {
        val namespaces = nameSpaceQueryService.getAllNameSpaces()
        return ResponseEntity.ok(namespaces)
    }
}
