package infrastructure

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Main Spring Boot application class for the MS Suggestions service.
 *
 * This class is intentionally empty; Spring Boot bootstraps the application
 * via the `runApplication` call in the `main` function.
 */
@SpringBootApplication
class MsSuggestionsApplication

/**
 * Application entry point. Starts the Spring context.
 */
fun main(args: Array<String>) {
    runApplication<MsSuggestionsApplication>(*args)
}