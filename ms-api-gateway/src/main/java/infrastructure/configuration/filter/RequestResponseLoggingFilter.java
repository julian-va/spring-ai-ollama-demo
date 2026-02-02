package infrastructure.configuration.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static infrastructure.configuration.ApplicationConstants.*;

/**
 * Global WebFlux filter that logs incoming HTTP requests and outgoing responses
 * for the Spring Cloud Gateway.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Inject a request id header when missing.</li>
 *   <li>Log a concise incoming request line with method, path, id, remote
 *       address, query parameters and headers (sensitive header values are
 *       masked).</li>
 *   <li>After downstream processing, log response status and elapsed time.</li>
 * </ul>
 *
 * <p>This class is a Spring {@code @Component} and implements {@link GlobalFilter}
 * so it is applied to all routed requests through the gateway.
 */
@Component
@Slf4j
public class RequestResponseLoggingFilter implements GlobalFilter, Ordered {

    /**
     * Filter implementation that logs an incoming request and the corresponding
     * response once processing completes.
     *
     * @param exchange the current server web exchange containing request/response
     * @param chain    the filter chain to delegate to
     * @return a {@link Mono} that indicates when request processing is complete
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange serverWebExchange = exchange.mutate().request(builder -> builder.header(X_REQUEST_ID_HEADER, UUID.randomUUID().toString()).build()).build();
        long startTime = System.nanoTime();
        logIncomingRequest(serverWebExchange);
        return chain.filter(serverWebExchange).then(Mono.fromRunnable(() -> logResponse(serverWebExchange, startTime)));
    }

    private void logIncomingRequest(ServerWebExchange exchange) {
        String requestPath = exchange.getRequest().getURI().getPath();
        String method = methodOrUnknown(exchange);
        String requestId = exchange.getRequest().getId();
        String queryParams = exchange.getRequest().getQueryParams().toString();
        String remoteAddr = resolveRemoteAddress(exchange.getRequest().getRemoteAddress());
        HttpHeaders headers = exchange.getRequest().getHeaders();

        log.info(LOG_INCOMING_TEMPLATE, method, requestPath, requestId, remoteAddr, queryParams, sanitizedHeaders(headers));
    }

    private String sanitizedHeaders(HttpHeaders headers) {
        if (Objects.isNull(headers)) {
            return EMPTY_MAP_REPR;
        }
        Map<String, String> sanitizedHeadersMap = headers.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            String headerNameLower = entry.getKey().toLowerCase();
                            String valuesJoined = String.join(CSV_SEPARATOR, entry.getValue());
                            return SENSITIVE_HEADERS.contains(headerNameLower) ? maskValue(valuesJoined) : valuesJoined;
                        }
                ));
        return sanitizedHeadersMap.toString();
    }

    private String maskValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return MASKED_VALUE;
        }
        if (value.length() <= VISIBLE_CHARS * 2) {
            return MASKED_VALUE;
        }
        String prefix = Strings.concat(value.substring(0, VISIBLE_CHARS), MASK_MID);
        String suffix = value.substring(value.length() - VISIBLE_CHARS);
        return Strings.concat(prefix, suffix);
    }

    private String resolveRemoteAddress(InetSocketAddress socketAddress) {
        if (Objects.isNull(socketAddress)) {
            return UNKNOWN;
        }
        InetAddress inet = socketAddress.getAddress();
        if (Objects.nonNull(inet)) {
            return inet.getHostAddress() + COLON + socketAddress.getPort();
        } else {
            return socketAddress.toString();
        }
    }

    private void logResponse(ServerWebExchange exchange, long startTime) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        Integer status = Objects.nonNull(exchange.getResponse().getStatusCode())
                ? exchange.getResponse().getStatusCode().value()
                : null;
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        log.info(LOG_RESPONSE_TEMPLATE, methodOrUnknown(exchange), exchange.getRequest().getURI().getPath(),
                status != null ? status : UNKNOWN, elapsed, sanitizedHeaders(headers));
    }

    private String methodOrUnknown(ServerWebExchange exchange) {
        return exchange.getRequest().getMethod().name();
    }


    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
