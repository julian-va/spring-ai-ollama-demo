package application.exception

import org.springframework.http.HttpStatus

open class UseCaseException(message: String, cause: Throwable? = null, val httpStatus: HttpStatus) :
    RuntimeException(message, cause)