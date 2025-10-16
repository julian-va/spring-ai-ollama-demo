package jva.cloud.infrastructure

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MsSuggestionsApplication

fun main(args: Array<String>) {
    runApplication<MsSuggestionsApplication>(*args)
}