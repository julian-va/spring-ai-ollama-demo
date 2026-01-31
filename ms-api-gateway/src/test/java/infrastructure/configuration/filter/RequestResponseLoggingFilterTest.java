package infrastructure.configuration.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static infrastructure.configuration.LoggingConstants.X_REQUEST_ID_HEADER;
import static org.junit.jupiter.api.Assertions.*;

public class RequestResponseLoggingFilterTest {

    @Test
    void filter_should_add_request_id_header_and_preserve_request_processing() {
        RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter();
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            captured.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        ServerWebExchange used = captured.get();
        assertNotNull(used);
        String requestId = used.getRequest().getHeaders().getFirst(X_REQUEST_ID_HEADER);
        assertNotNull(requestId);
        // should be a valid UUID
        UUID.fromString(requestId);
    }

    @Test
    void sanitizedHeaders_masks_sensitive_headers() throws Exception {
        RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer abcdefghijklmnopqrstuvwxyz1234567890");
        headers.add("Content-Type", "application/json");

        Method method = RequestResponseLoggingFilter.class.getDeclaredMethod("sanitizedHeaders", HttpHeaders.class);
        method.setAccessible(true);
        String result = (String) method.invoke(filter, headers);

        assertNotNull(result);
        assertTrue(result.contains("Content-Type="));
        assertTrue(result.toLowerCase().contains("authorization="));
        // Ensure the full token is not present
        assertFalse(result.contains("abcdefghijklmnopqrstuvwxyz1234567890"));
        // Check masked pattern (prefix 4 and suffix 4 are visible with '...')
        assertTrue(result.contains("Bear...7890"));
    }
}
