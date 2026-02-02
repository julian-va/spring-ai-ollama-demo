package infrastructure.configuration;

import java.util.Set;

/**
 * General application-wide constants.
 *
 * <p>This class centralizes commonly used string constants, header names and
 * configuration keys so they can be reused across the codebase.</p>
 */
public final class ApplicationConstants {

    /**
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

    // Additional application constants
    public static final String PUBLIC_PATH = "/public/**";
    public static final String OLLAMA_USER_ROLE = "ollama-user";
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String REALM_ACCESS_CLAIM = "realm_access";
    public static final String ROLES_CLAIM = "roles";
    public static final String ACTUATOR_HEALTH_PATH = "/actuator/health";

    private ApplicationConstants() {
        // prevent instantiation
    }
}
