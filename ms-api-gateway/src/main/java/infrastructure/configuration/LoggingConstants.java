package infrastructure.configuration;

import java.util.Set;

/**
 * Centralized logging-related constants shared across filters and logging utilities.
 *
 * <p>This class groups string templates, header names and masking configuration
 * so they can be reused by multiple components without duplicating literals.
 */
public final class LoggingConstants {

    /**
     * prefix + "..." + suffix;
     * Headers that must have their values masked in logs.
     */
    public static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-auth-token",
            "x-api-key"
    );

    public static final String X_REQUEST_ID_HEADER = "X-Request-ID";
    public static final String LOG_INCOMING_TEMPLATE = "Incoming request: method={} path={} id={} remoteAddress={} query={} headers={}";
    public static final String LOG_RESPONSE_TEMPLATE = "[Gateway Response] {} {} -> {} ({} ms) headers={}";
    public static final String EMPTY_MAP_REPR = "{}";
    public static final String CSV_SEPARATOR = ",";
    public static final String MASKED_VALUE = "****";
    public static final String MASK_MID = "...";
    public static final int VISIBLE_CHARS = 4;
    public static final String COLON = ":";
    public static final String UNKNOWN = "unknown";

    private LoggingConstants() {
        // prevent instantiation
    }
}
