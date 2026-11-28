package fr.marwen.kanban.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Anthropic HTTP client configuration — gate #1 of the three-gate AI bean graph (AD-5 revised,
 * AD-6 graceful degradation).
 *
 * <p>Loads only when {@code anthropic.api-key} is set AND non-blank. Patch A (review pass 1):
 * {@code @ConditionalOnProperty} without {@code havingValue} would match any value except the
 * literal {@code "false"}, which lets an empty-string key (either an unset env var hitting
 * {@code ${ANTHROPIC_API_KEY:}} or an explicit {@code anthropic.api-key=""}) load the full AI
 * bean graph. {@code /api/config} would then report {@code aiEnabled: true} while every
 * breakdown fails 503 upstream — the honesty invariant (CAP-7) breaks. The SpEL predicate
 * {@code '${anthropic.api-key:}'.trim() != ''} rejects blank values at bean-registration time.
 * {@link AiBreakdownService}, {@link AiBreakdownValidator}, and {@link AiBreakdownController}
 * all carry the same predicate so the gates stay in lockstep.
 *
 * <p>Timeouts: 10 s connect / 30 s read per AD-5, wired via Spring Boot 4.1's
 * {@link HttpClientSettings} (renamed from {@code ClientHttpRequestFactorySettings} in Spring
 * Boot 4) and {@link ClientHttpRequestFactoryBuilder#detect()} which picks the JDK
 * {@code java.net.http.HttpClient} on Java 21 — HTTP/2-capable, per-request total timeout
 * (not per-socket idle), and no {@code http.maxConnections=5} cap. No retry, no circuit
 * breaker — single-shot to fit NFR-2 (p95 &lt; 10 s).
 */
@Configuration
@ConditionalOnExpression(AnthropicRestClientConfig.KEY_PRESENT_EXPRESSION)
public class AnthropicRestClientConfig {

    /**
     * Shared SpEL for the four {@code @ConditionalOnExpression} gates (patch A). The trimmed
     * check rejects whitespace-only keys too. Public so sibling-package beans
     * ({@link AiBreakdownService}, {@code AiBreakdownValidator}, {@code AiBreakdownController})
     * can reference it without duplicating the literal.
     */
    public static final String KEY_PRESENT_EXPRESSION = "#{'${anthropic.api-key:}'.trim() != ''}";

    static final String BASE_URL = "https://api.anthropic.com";
    static final String ANTHROPIC_VERSION = "2026-10-21";
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    /** Public so {@link AiBreakdownService} can report the exact timeout in its WARN log. */
    public static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Named {@code "anthropicRestClient"} so a future second RestClient (none in scope) wouldn't
     * conflict on the generic {@link RestClient} type. Injects Spring Boot's auto-configured
     * {@link RestClient.Builder} so Jackson 3 message converters are wired — the service can
     * then pass a {@code Map} body and let Jackson own serialization end-to-end (patch W).
     * {@link HttpHeaders#ACCEPT} default (patch E's fold-in of T) makes JSON intent explicit.
     */
    @Bean
    public RestClient anthropicRestClient(
            RestClient.Builder builder,
            @Value("${anthropic.api-key}") String apiKey) {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withReadTimeout(READ_TIMEOUT);
        ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.detect().build(settings);
        return builder
                .baseUrl(BASE_URL)
                .requestFactory(factory)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
