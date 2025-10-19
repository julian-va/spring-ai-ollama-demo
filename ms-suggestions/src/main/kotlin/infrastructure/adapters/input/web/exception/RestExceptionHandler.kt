package infrastructure.adapters.input.web.exception

import application.exception.UseCaseException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import domain.exception.BusinessRuleViolationException
import infrastructure.adapters.dto.ErrorResponse
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice
class RestExceptionHandler {
    private val logger = LoggerFactory.getLogger(RestExceptionHandler::class.java)

    @ExceptionHandler(value = [WebExchangeBindException::class, MethodArgumentNotValidException::class, BindException::class])
    fun handleBindingExceptions(e: Exception): ResponseEntity<ErrorResponse> {
        val fieldErrors = when (e) {
            is WebExchangeBindException -> e.bindingResult.fieldErrors
            is MethodArgumentNotValidException -> e.bindingResult.fieldErrors
            is BindException -> e.bindingResult.fieldErrors
            else -> emptyList()
        }
        val errorsList = fieldErrors
            .map { String.format(FIELD_ERROR_TEMPLATE, it.field, it.defaultMessage ?: GENERIC_ERROR_MESSAGE) }
            .ifEmpty { listOf(GENERIC_ERROR_MESSAGE) }
        val joined = errorsList.joinToString(separator = JOIN_SEPARATOR)
        logger.warn(LOG_VALIDATION_FAILED, joined, e)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(VALIDATION_FAILED_MESSAGE, errorsList))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(e: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        val errorsList = e.constraintViolations
            .map { violation ->
                val path = violation.propertyPath?.toString()?.substringAfterLast('.') ?: ""
                val msg = violation.message ?: GENERIC_ERROR_MESSAGE
                if (path.isBlank()) msg else String.format(FIELD_ERROR_TEMPLATE, path, msg)
            }
            .ifEmpty { listOf(GENERIC_ERROR_MESSAGE) }
        val joined = errorsList.joinToString(separator = JOIN_SEPARATOR)
        logger.warn(LOG_CONSTRAINT_VIOLATIONS, joined, e)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(VALIDATION_FAILED_MESSAGE, errorsList))
    }

    @ExceptionHandler(BusinessRuleViolationException::class)
    fun handleBusiness(e: BusinessRuleViolationException): ResponseEntity<ErrorResponse> {
        logger.warn(LOG_BUSINESS_VIOLATION, e.message, e)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(e.message ?: BUSINESS_VIOLATION_MESSAGE))
    }

    @ExceptionHandler(value = [UseCaseException::class])
    fun handleUseCase(exception: UseCaseException): ResponseEntity<ErrorResponse> {
        logger.warn(LOG_USECASE, exception.httpStatus, exception.message)
        return ResponseEntity.status(exception.httpStatus)
            .body(ErrorResponse(exception.message ?: USE_CASE_ERROR_MESSAGE))
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(e: ResponseStatusException): ResponseEntity<ErrorResponse> {
        logger.warn(LOG_RESPONSE_STATUS, e.statusCode, e.reason, e)
        val message: String = e.reason?.takeIf { it.isNotBlank() } ?: e.message
        return ResponseEntity.status(e.statusCode).body(ErrorResponse(message))
    }

    @ExceptionHandler(ServerWebInputException::class)
    fun handleServerWebInput(e: ServerWebInputException): ResponseEntity<ErrorResponse> {
        logger.warn(LOG_FAILED_READ_HTTP, e.message, e)
        // Prefer to map common causes to friendly validation messages instead of leaking stack traces
        val cause = e.cause
        val bodyMessage: String
        val errorsList: List<String>?
        when (cause) {
            is WebExchangeBindException -> {
                val list = cause.bindingResult.fieldErrors
                    .map { String.format(FIELD_ERROR_TEMPLATE, it.field, it.defaultMessage ?: GENERIC_ERROR_MESSAGE) }
                    .ifEmpty { listOf(GENERIC_ERROR_MESSAGE) }
                bodyMessage = VALIDATION_FAILED_MESSAGE
                errorsList = list
            }

            is MismatchedInputException -> {
                // Try to extract missing property name from the exception path
                val lastRef = cause.path.lastOrNull()
                val missingName = lastRef?.fieldName ?: lastRef?.toString() ?: REQUIRED_PROPERTY_FALLBACK
                bodyMessage = String.format(MISSING_REQUIRED_PROPERTY_TEMPLATE, missingName)
                errorsList = listOf(missingName)
            }

            else -> {
                bodyMessage = GENERIC_ERROR_MESSAGE
                errorsList = null
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(bodyMessage, errorsList))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error(LOG_UNHANDLED_EXCEPTION, e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse(INTERNAL_ERROR_MESSAGE))
    }

    companion object {
        private const val BUSINESS_VIOLATION_MESSAGE = "business rule violation"
        private const val USE_CASE_ERROR_MESSAGE = "use case error"
        private const val GENERIC_ERROR_MESSAGE = "error"
        private const val INTERNAL_ERROR_MESSAGE = "internal error"

        // Moved hard-coded response texts to constants
        private const val VALIDATION_FAILED_MESSAGE = "Validation failed"
        private const val MISSING_REQUIRED_PROPERTY_TEMPLATE = "Missing required property: %s"
        private const val REQUIRED_PROPERTY_FALLBACK = "required property"

        // Additional constants for remaining hard-coded texts
        private const val FIELD_ERROR_TEMPLATE = "%s: %s"
        private const val JOIN_SEPARATOR = "; "

        private const val LOG_VALIDATION_FAILED = "Validation failed: {}"
        private const val LOG_CONSTRAINT_VIOLATIONS = "Constraint violations: {}"
        private const val LOG_BUSINESS_VIOLATION = "Business rule violation: {}"
        private const val LOG_USECASE = "UseCaseException details: status={}, message={}"
        private const val LOG_RESPONSE_STATUS = "ResponseStatusException -> status={}, reason={}"
        private const val LOG_FAILED_READ_HTTP = "Failed to read HTTP message: {}"
        private const val LOG_UNHANDLED_EXCEPTION = "Unhandled exception"
    }
}