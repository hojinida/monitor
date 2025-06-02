package com.example.monitor.domain.error

class CustomException(
    override val message: String
) : RuntimeException(message)
