package infrastructure.adapters.input.web

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Simple health-check controller exposing a ping endpoint.
 *
 * Returns a plain text "pong" response with HTTP 200 to allow load
 * balancers or uptime checks to verify the application is alive.
 */
@RestController
@RequestMapping(value = ["/ping"])
class PingController {

    /**
     * Respond with a plain "pong" text to indicate the service is reachable.
     *
     * @return HTTP 200 with body "pong"
     */
    @GetMapping(produces = [MediaType.TEXT_PLAIN_VALUE])
    fun ping(): ResponseEntity<String> = ResponseEntity.ok("pong")
}

