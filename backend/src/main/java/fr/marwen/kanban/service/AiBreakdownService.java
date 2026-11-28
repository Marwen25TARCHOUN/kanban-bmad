package fr.marwen.kanban.service;

import fr.marwen.kanban.config.AnthropicRestClientConfig;
import fr.marwen.kanban.dto.BreakdownResponse.CardDraft;
import fr.marwen.kanban.exception.AiUnavailableException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Gate #2 of the three-gate AI bean graph (AD-5 revised) — orchestrates the Anthropic Messages
 * API call that powers {@code POST /api/ai/breakdown} (CAP-2).
 *
 * <p>Flow: build the structured-output request body from {@link #PROMPT_TEMPLATE} +
 * {@link #RESPONSE_SCHEMA} (parsed once in the constructor, patch M), POST to
 * {@value #MESSAGES_URI} with a {@code Map} body so Jackson owns serialization (patch W), parse
 * the envelope, extract the {@code cards} array, hand it to {@link AiBreakdownValidator}, return
 * the validated drafts. Single-shot, no retry (NFR-2 10 s p95 budget).
 *
 * <p>Error handling (AD-2 hard split; patch G/B refined):
 * <ul>
 *   <li>{@code HttpClientErrorException.Unauthorized} → WARN {@code "Anthropic auth failed"} +
 *       {@link AiUnavailableException}. The spec AC checks for that exact phrase.</li>
 *   <li>Any other {@link HttpStatusCodeException} (429, 5xx, …) → WARN {@code "Anthropic call
 *       failed"} with class + status code, never the response body (patch B — Spring's rendered
 *       message embeds the response body and could echo back the {@code x-api-key} header).</li>
 *   <li>{@link ResourceAccessException} wrapping {@link SocketTimeoutException} → WARN
 *       {@code "Anthropic read timeout"} with the configured timeout. Spec AC phrase.</li>
 *   <li>Any other {@link ResourceAccessException} / {@link RestClientException} → generic WARN
 *       "network failure" with the class name only.</li>
 *   <li>Response parse / size-cap / envelope / validator failures → {@link AiUnavailableException}
 *       with a short reason, no response body.</li>
 * </ul>
 *
 * <p>Response size guard (patch R): bodies over 1 MiB are rejected before parsing to cap the
 * worst-case heap pressure on a hostile upstream.
 */
@Service
@ConditionalOnExpression(AnthropicRestClientConfig.KEY_PRESENT_EXPRESSION)
@Slf4j
public class AiBreakdownService {

    static final String MODEL = "claude-opus-4-7";
    static final int MAX_TOKENS = 1500;
    static final double TEMPERATURE = 0.3;
    static final String MESSAGES_URI = "/v1/messages";

    /** Patch R: 1 MiB cap on the Anthropic response body before we hand it to Jackson. */
    static final int MAX_RESPONSE_BYTES = 1 << 20;

    /**
     * Q1 landed B (2026-11-27): structured mid-weight prompt. Reproduced VERBATIM from the
     * spec's Design Notes so round-trip tests stay deterministic. {@code {locale}} is substituted
     * at call time with "English" or "French".
     */
    static final String PROMPT_TEMPLATE = """
            You are a project-breakdown assistant. Given a short idea, produce 4–6 Kanban cards in {locale}.

            Rules:
            1. Title: imperative verb phrase, ≤120 characters. No trailing punctuation.
            2. Description: 1–2 sentences explaining what the task covers and why it's needed. ≤2000 characters.
            3. Priority: HIGH for cards that unlock downstream work (foundations, auth, setup); MEDIUM for core work;
               LOW for polish, docs, or follow-up tasks.
            4. Avoid vague verbs like "set up stuff" or "configure things" — name concrete artifacts.

            Example — idea: "Deploy my app to Fly.io":
              - "Set up Fly.io account and CLI" / MEDIUM / Install flyctl, authenticate, verify free-tier access.
              - "Write Dockerfile + .dockerignore" / HIGH / Multi-stage build, non-root user, slim base image.
              - "Configure fly.toml" / MEDIUM / Primary region, health checks, autoscaling, env vars.
              - "Test deploy and rollback" / MEDIUM / Deploy, verify response, trigger rollback, confirm reversal.

            Return your answer as the structured JSON schema, nothing else.
            """;

    /**
     * JSON schema pushed to Anthropic in {@code output_config.format}. Reproduced VERBATIM from
     * the spec's Design Notes — the server and the model MUST agree on shape. The validator
     * re-checks every rule post-flight so drift between the schema and model output doesn't
     * silently produce a bad response.
     */
    static final String RESPONSE_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "cards": {
                  "type": "array",
                  "minItems": 4,
                  "maxItems": 6,
                  "items": {
                    "type": "object",
                    "properties": {
                      "title":       {"type": "string", "minLength": 1, "maxLength": 120},
                      "description": {"type": "string", "minLength": 1, "maxLength": 2000},
                      "priority":    {"type": "string", "enum": ["LOW", "MEDIUM", "HIGH"]}
                    },
                    "required": ["title", "description", "priority"]
                  }
                }
              },
              "required": ["cards"]
            }
            """;

    private final RestClient restClient;
    private final AiBreakdownValidator validator;
    private final ObjectMapper objectMapper;
    private final JsonNode schemaNode;

    public AiBreakdownService(
            RestClient anthropicRestClient,
            AiBreakdownValidator validator,
            ObjectMapper objectMapper) {
        this.restClient = anthropicRestClient;
        this.validator = validator;
        this.objectMapper = objectMapper;
        // Patch M: parse the schema once at bean init. Bean construction fails fast if someone
        // breaks RESPONSE_SCHEMA — better than a 503 on every call.
        try {
            this.schemaNode = objectMapper.readTree(RESPONSE_SCHEMA);
        } catch (JacksonException ex) {
            throw new IllegalStateException(
                    "RESPONSE_SCHEMA is not valid JSON — fix the constant", ex);
        }
    }

    public List<CardDraft> breakdown(String idea, String locale) {
        String languageName = "fr".equals(locale) ? "French" : "English";
        String systemPrompt = PROMPT_TEMPLATE.replace("{locale}", languageName);

        // Patch W: pass a Map body so Jackson 3 (the auto-configured converter on the builder
        // we received) owns serialization. No StringHttpMessageConverter ordering dependency.
        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "max_tokens", MAX_TOKENS,
                "temperature", TEMPERATURE,
                "system", systemPrompt,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", idea)),
                "output_config", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "schema", schemaNode)));

        String responseBody;
        try {
            responseBody = restClient.post()
                    .uri(MESSAGES_URI)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.Unauthorized ex) {
            // Patch G: spec AC promises WARN "Anthropic auth failed". Only the status is logged,
            // never the response body (patch B — Spring's getMessage() embeds the body which may
            // echo the x-api-key header).
            log.warn("Anthropic auth failed: status={}", ex.getStatusCode());
            throw new AiUnavailableException("Anthropic auth failed", ex);
        } catch (HttpStatusCodeException ex) {
            log.warn("Anthropic call failed: class={} status={}",
                    ex.getClass().getSimpleName(), ex.getStatusCode());
            throw new AiUnavailableException("Anthropic upstream status", ex);
        } catch (ResourceAccessException ex) {
            // Patch G: SocketTimeoutException cause → distinct "Anthropic read timeout" phrase
            // the spec AC checks for. Other network errors (UnknownHostException, ConnectException,
            // …) get a generic network-failure line.
            if (ex.getCause() instanceof SocketTimeoutException) {
                log.warn("Anthropic read timeout: timeout={}ms",
                        AnthropicRestClientConfig.READ_TIMEOUT.toMillis());
            } else {
                String causeClass = ex.getCause() != null
                        ? ex.getCause().getClass().getSimpleName()
                        : "n/a";
                log.warn("Anthropic network failure: class={} cause={}",
                        ex.getClass().getSimpleName(), causeClass);
            }
            throw new AiUnavailableException("Anthropic network failure", ex);
        } catch (RestClientException ex) {
            // Patch B: still never log getMessage() — a UnknownContentTypeException / etc. will
            // carry the body in its message.
            log.warn("Anthropic call failed: class={}", ex.getClass().getSimpleName());
            throw new AiUnavailableException("Anthropic call failed", ex);
        }

        if (responseBody == null || responseBody.isBlank()) {
            log.warn("Anthropic returned empty body");
            throw new AiUnavailableException("Anthropic returned empty body");
        }
        if (responseBody.length() > MAX_RESPONSE_BYTES) {
            // Patch R: cheap guard against a hostile / misconfigured upstream that would
            // otherwise balloon memory inside Jackson's tree parser. The limit is deliberately
            // generous for a legit structured-output payload.
            log.warn("Anthropic response exceeded size limit: length={}", responseBody.length());
            throw new AiUnavailableException("Anthropic response exceeds size limit");
        }

        JsonNode responseRoot;
        try {
            responseRoot = objectMapper.readTree(responseBody);
        } catch (JacksonException ex) {
            log.warn("Anthropic response is not JSON: class={}", ex.getClass().getSimpleName());
            throw new AiUnavailableException("Anthropic response is not JSON", ex);
        }

        JsonNode cardsNode = extractCards(responseRoot);
        return validator.validate(cardsNode);
    }

    /**
     * Pull the {@code cards} array out of the Messages API envelope.
     *
     * <p>Primary path: {@code content[0].input.cards} — the API-defined slot for
     * {@code type: "json_schema"} structured output under {@code anthropic-version: 2026-10-21}.
     *
     * <p>Fallback: {@code content[0].text} with markdown code fences stripped (patch K), parsed
     * as JSON, extracting its {@code cards} field. A fallback hit is WARN-logged so a reviewer
     * sees the format inconsistency instead of a silently working-around parser.
     */
    private JsonNode extractCards(JsonNode root) {
        JsonNode firstContent = root.path("content").path(0);

        JsonNode input = firstContent.path("input");
        if (input.isObject() && input.has("cards")) {
            return input.get("cards");
        }

        JsonNode text = firstContent.path("text");
        if (text.isTextual()) {
            log.warn("Anthropic structured output missing from content[0].input; "
                    + "falling back to content[0].text parse");
            String cleaned = stripMarkdownFences(text.asText());
            try {
                JsonNode fallback = objectMapper.readTree(cleaned);
                if (fallback.isObject() && fallback.has("cards")) {
                    return fallback.get("cards");
                }
            } catch (JacksonException ex) {
                throw new AiUnavailableException(
                        "fallback text parse failed: class=" + ex.getClass().getSimpleName(), ex);
            }
        }

        throw new AiUnavailableException("response envelope has no cards payload");
    }

    /**
     * Patch K: strip leading / trailing {@code ```json … ```} fences. LLM structured output
     * occasionally wraps its answer in a markdown block even when told not to; this lets the
     * fallback path survive that.
     */
    private static String stripMarkdownFences(String raw) {
        String cleaned = raw
                .replaceFirst("(?s)^\\s*```(?:json)?\\s*", "")
                .replaceFirst("(?s)\\s*```\\s*$", "");
        return cleaned.strip();
    }
}
