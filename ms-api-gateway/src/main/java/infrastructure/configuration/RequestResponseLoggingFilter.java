package infrastructure.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Global filter that logs incoming HTTP requests and outgoing responses for the
 * Spring Cloud Gateway. It records basic request metadata (HTTP method, path,
 * request id, remote address and query parameters) when the request arrives,
 * and logs the response status and total elapsed time in milliseconds after
 * the downstream processing completes.
 * <p>
 * This filter uses the lowest precedence so it runs late in the filter chain.
 */
@Component
@Slf4j
public class RequestResponseLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.nanoTime();
        logIncomingRequest(exchange);
        return chain.filter(exchange).then(Mono.fromRunnable(() -> logResponse(exchange, startTime)));
    }

    /**
     * Extracts request information from the exchange and logs a concise incoming
     * request line including method, path, request id, remote address and query
     * parameters.
     *
     * @param exchange the current server web exchange containing the request
     */
    private void logIncomingRequest(ServerWebExchange exchange) {
        String requestPath = exchange.getRequest().getURI().getPath();
        String method = methodOrUnknown(exchange);
        String requestId = exchange.getRequest().getId();
        String queryParams = exchange.getRequest().getQueryParams().toString();
        String remoteAddr = resolveRemoteAddress(exchange.getRequest().getRemoteAddress());

        log.info("Incoming request: method={} path={} id={} remoteAddress={} query={}", method, requestPath, requestId, remoteAddr, queryParams);
    }

    /**
     * Resolve a human-readable remote address from an InetSocketAddress. If the
     * socket address is null, returns "unknown". If the underlying InetAddress
     * is available, returns "host:port"; otherwise falls back to
     * InetSocketAddress.toString().
     *
     * @param socketAddress the remote socket address (maybe null)
     * @return resolved remote address string
     */
    private String resolveRemoteAddress(InetSocketAddress socketAddress) {
        if (Objects.isNull(socketAddress)) {
            return "unknown";
        }
        InetAddress inet = socketAddress.getAddress();
        if (Objects.nonNull(inet)) {
            return inet.getHostAddress() + ":" + socketAddress.getPort();
        } else {
            return socketAddress.toString();
        }
    }

    /**
     * Log response status and elapsed time since provided startTime.
     *
     * @param exchange  the current server web exchange containing the response
     * @param startTime the start time in nanoseconds used to compute elapsed ms
     */
    private void logResponse(ServerWebExchange exchange, long startTime) {
        Integer status = Objects.nonNull(exchange.getResponse().getStatusCode())
                ? exchange.getResponse().getStatusCode().value()
                : null;
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        log.info("[Gateway Response] {} {} -> {} ({} ms)", methodOrUnknown(exchange), exchange.getRequest().getURI().getPath(),
                status != null ? status : "unknown", elapsed);
    }

    /**
     * Return the request HTTP method name or "UNKNOWN" if not present. The
     * method exists as a single point of truth to avoid repeating null checks.
     *
     * @param exchange the current server web exchange containing the request
     * @return HTTP method name or "UNKNOWN"
     */
    private String methodOrUnknown(ServerWebExchange exchange) {
        return exchange.getRequest().getMethod().name();
    }


    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
