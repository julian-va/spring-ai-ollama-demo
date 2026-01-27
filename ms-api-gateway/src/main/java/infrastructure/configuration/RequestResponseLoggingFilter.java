package infrastructure.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RequestResponseLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.nanoTime();
        String requestPath = exchange.getRequest().getURI().getPath();
        log.info("Incoming request: {} {} ", exchange.getRequest().getMethod(), requestPath);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Integer status = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value()
                    : null;
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            log.info("[Gateway Response] {} {} -> {} ({} ms)", exchange.getRequest().getMethod(), requestPath,
                    status != null ? status : "unknown", elapsed);
        }));
    }


    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
