package com.example.monitor.presentation.controller

import com.example.monitor.application.deployment.DeploymentService
import com.example.monitor.application.deployment.dto.DeploymentResponse
import com.example.monitor.presentation.dto.ImageRequest
import com.example.monitor.presentation.dto.ScaleRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/deployments")
class DeploymentController(
    private val deploymentService: DeploymentService
) {

    @GetMapping("/{namespace}")
    fun getDeploymentsByNamespace(
        @PathVariable namespace: String
    ): ResponseEntity<List<DeploymentResponse>> {
        val responses = deploymentService.getDeploymentsInNamespace(namespace)
        return ResponseEntity.ok(responses)
    }

    @PatchMapping("/scale")
    fun scaleDeployment(
        @RequestBody req: ScaleRequest
    ): ResponseEntity<Unit> {
        deploymentService.updateReplicas(req.namespace, req.name, req.replicas)
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/image")
    fun updateDeploymentImage(
        @RequestBody req: ImageRequest
    ): ResponseEntity<Unit> {
        deploymentService.updateImage(req.namespace, req.name, req.imageTag)
        return ResponseEntity.ok().build()
    }
}
