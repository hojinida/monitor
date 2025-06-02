package com.example.monitor.presentation.handler

import com.example.monitor.domain.error.CustomException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(e: CustomException): ResponseEntity<ApiError> {
        val body = ApiError(
            message = e.message ?: "Unknown error"
        )
        return ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    fun handleOtherExceptions(e: Exception): ResponseEntity<ApiError> {
        val body = ApiError(
            message = e.message ?: "Internal server error"
        )
        return ResponseEntity(body, HttpStatus.INTERNAL_SERVER_ERROR)
    }

}

data class ApiError(
    val message: String
)
