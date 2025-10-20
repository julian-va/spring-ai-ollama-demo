package domain.exception

class BusinessRuleViolationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)