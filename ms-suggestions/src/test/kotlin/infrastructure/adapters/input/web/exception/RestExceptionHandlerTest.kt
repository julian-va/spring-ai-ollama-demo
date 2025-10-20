package infrastructure.adapters.input.web.exception

import application.exception.UseCaseException
import domain.exception.BusinessRuleViolationException
import jakarta.validation.ConstraintViolationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.validation.BindException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebInputException

class RestExceptionHandlerTest {
    private val handler = RestExceptionHandler()

    @Test
    fun `handle business rule returns 400 with message`() {
        val ex = BusinessRuleViolationException("business failed")
        val response = handler.handleBusiness(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.message).isEqualTo("business failed")
    }

    @Test
    fun `handle use case returns http status and message`() {
        val ex = UseCaseException("use case failure", null, HttpStatus.CONFLICT)
        val response = handler.handleUseCase(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body?.message).isEqualTo("use case failure")
    }

    @Test
    fun `handle response status uses reason when present`() {
        val ex = ResponseStatusException(HttpStatus.NOT_FOUND, "not found")
        val response = handler.handleResponseStatus(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body?.message).isEqualTo("not found")
    }

    @Test
    fun `handle server web input without cause returns generic error`() {
        val ex = ServerWebInputException("failed")
        val response = handler.handleServerWebInput(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.message).isEqualTo("error")
    }

    @Test
    fun `handle bind exception returns validation failed with generic error`() {
        val bind = BindException(Any(), "obj")
        val response = handler.handleBindingExceptions(bind)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.message).isEqualTo("Validation failed")
        assertThat(response.body?.errors).isNotNull
        assertThat(response.body?.errors).containsExactly("error")
    }

    @Test
    fun `handle constraint violation with empty set returns validation failed`() {
        val cv = ConstraintViolationException(emptySet())
        val response = handler.handleConstraintViolation(cv)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.message).isEqualTo("Validation failed")
        assertThat(response.body?.errors).isNotNull
        assertThat(response.body?.errors).containsExactly("error")
    }

    @Test
    fun `handle generic returns internal error`() {
        val response = handler.handleGeneric(RuntimeException("boom"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body?.message).isEqualTo("internal error")
    }
}
