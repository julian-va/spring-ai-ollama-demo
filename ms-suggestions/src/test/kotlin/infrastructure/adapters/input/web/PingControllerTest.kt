package infrastructure.adapters.input.web

import jva.cloud.infrastructure.adapters.input.web.PingController
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@DisplayName("Ping Controller Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PingControllerTest {

    private lateinit var client: WebTestClient

    @BeforeAll
    fun setup() {
        val controller = PingController()
        client = WebTestClient.bindToController(controller).build()
    }

    @Test
    @DisplayName("GET /ping returns plain pong")
    fun `ping returns pong plain text`() {
        client.get()
            .uri("/ping")
            .accept(MediaType.TEXT_PLAIN)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String::class.java).isEqualTo("pong")
    }
}
