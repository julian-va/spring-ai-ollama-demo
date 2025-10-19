package jva.cloud.infrastructure.adapters.input.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PingControllerTest {

    private lateinit var controller: PingController

    @BeforeEach
    fun setUp() {
        controller = PingController()
    }

    @Test
    fun `ping returns pong`() {
        val response = controller.ping()

        assertEquals(200, response.statusCodeValue)
        assertEquals("pong", response.body)
    }
}
