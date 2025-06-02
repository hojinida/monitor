package com.example.monitor.presentation.dto

data class ScaleRequest(
    val namespace: String, val name: String, val replicas: Int
)
