package infrastructure.configuration

import application.service.AiRecommenderService
import application.port.output.AiRecommenderPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring bean configuration for the application wiring.
 *
 * Provides beans that compose the use case implementations with their
 * required outbound ports.
 */
@Configuration
class BeanConfiguration {

    /**
     * Create the AiRecommenderService bean wired with the provided port.
     *
     * @param aiRecommenderPort outbound port implementation used by the service
     * @return a configured AiRecommenderService instance
     */
    @Bean
    fun aiRecommenderUseCase(aiRecommenderPort: AiRecommenderPort): AiRecommenderService {
        return AiRecommenderService(aiRecommenderPort = aiRecommenderPort)
    }
}