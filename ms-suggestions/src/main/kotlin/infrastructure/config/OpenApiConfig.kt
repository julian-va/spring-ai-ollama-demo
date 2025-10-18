package jva.cloud.infrastructure.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring configuration that exposes an OpenAPI / Swagger definition for the service.
 *
 * Provides a preconfigured {@link OpenAPI} bean including server URL, contact and
 * security scheme (bearer JWT) used by Springdoc.
 */
@Configuration
class OpenApiConfig(@param:Value("\${server.port:8080}") private val serverPort: String) {
    @Bean
    fun customOpenAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("MS Suggestions - Suggestions Service")
                    .version("1.0.0")
                    .description("Automatically generated documentation with Springdoc OpenAPI and Swagger UI")
                    .contact(
                        Contact()
                            .name("JVA Team")
                            .email("soporte@jva-cloud.example.com")
                            .url("https://jva-cloud.example.com")
                    )
                    .license(
                        License()
                            .name("Apache 2.0")
                            .url("http://www.apache.org/licenses/LICENSE-2.0.html")
                    )
            )
            .servers(
                listOf(
                    Server().url("http://localhost:$serverPort").description("Local development server")
                )
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            )
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
}