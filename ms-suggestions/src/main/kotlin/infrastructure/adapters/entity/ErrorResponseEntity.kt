package jva.cloud.infrastructure.adapters.entity

// Keep a message for quick clients and add an optional structured list of errors
data class ErrorResponse(val message: String, val errors: List<String>? = null)
