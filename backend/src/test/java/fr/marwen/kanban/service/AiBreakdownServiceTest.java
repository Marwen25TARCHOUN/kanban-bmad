package fr.marwen.kanban.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fr.marwen.kanban.domain.Priority;
import fr.marwen.kanban.dto.BreakdownResponse.CardDraft;
import fr.marwen.kanban.exception.AiUnavailableException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Patch D (review pass 1) — covers the Anthropic call orchestration that the spec's I/O matrix
 * demands. Uses {@link MockRestServiceServer} bound to a {@link RestClient.Builder} so the
 * service's HTTP layer stays unmocked; Jackson 3 serialization runs for real.
 *
 * <p>LogCaptor via Logback {@link ListAppender} — no new test-scope dep needed. Each test that
 * asserts on log content mounts an appender on the service's logger, then detaches in
 * {@link #afterEach}.
 *
 * <p>Coverage map (I/O matrix rows):
 * <ul>
 *   <li>happyPrimaryPath — content[0].input.cards</li>
 *   <li>happyFallbackPath — content[0].text</li>
 *   <li>fallbackStripsMarkdownFence — patch K</li>
 *   <li>upstream401 — WARN "Anthropic auth failed", no key in logs (patch G + VG9)</li>
 *   <li>upstream429 — generic AI_UNAVAILABLE</li>
 *   <li>upstream503 — generic AI_UNAVAILABLE</li>
 *   <li>nonJsonBody — AiUnavailableException</li>
 *   <li>emptyContentArray — AiUnavailableException with reason</li>
 *   <li>oversizedResponseBody — patch R</li>
 *   <li>bothInputAndText_primaryWins — EC9 verified-safe pin</li>
 *   <li>keyNeverLeaksIntoLogs — VG9 (LogCaptor)</li>
 * </ul>
 */
class AiBreakdownServiceTest {

    private static final String API_KEY = "test-key-REDACTEDMARKER42";

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final AiBreakdownValidator validator = new AiBreakdownValidator();

    private RestClient.Builder builder;
    private RestClient restClient;
    private MockRestServiceServer server;
    private AiBreakdownService service;

    private Logger serviceLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        this.builder = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("x-api-key", API_KEY)
                .defaultHeader("anthropic-version", "2026-10-21");
        this.server = MockRestServiceServer.bindTo(builder).build();
        this.restClient = builder.build();
        this.service = new AiBreakdownService(restClient, validator, objectMapper);

        this.serviceLogger = (Logger) LoggerFactory.getLogger(AiBreakdownService.class);
        this.logAppender = new ListAppender<>();
        logAppender.start();
        serviceLogger.addAppender(logAppender);
        serviceLogger.setLevel(Level.ALL);
    }

    @AfterEach
    void afterEach() {
        serviceLogger.detachAppender(logAppender);
        logAppender.stop();
        server.verify();
    }

    // ------------------------------------------------------------------- happy paths

    @Test
    void happyPrimaryPathReturnsFourValidatedDrafts() {
        String responseJson = """
                {"content":[{"input":{"cards":[
                  {"title":"T1","description":"d1","priority":"MEDIUM"},
                  {"title":"T2","description":"d2","priority":"HIGH"},
                  {"title":"T3","description":"d3","priority":"LOW"},
                  {"title":"T4","description":"d4","priority":"MEDIUM"}
                ]}}]}
                """;

        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", API_KEY))
                .andExpect(header("anthropic-version", "2026-10-21"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"model\":\"claude-opus-4-7\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"max_tokens\":1500")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"temperature\":0.3")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"role\":\"user\"")))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<CardDraft> drafts = service.breakdown("Deploy to Fly.io", "en");

        assertThat(drafts).hasSize(4);
        assertThat(drafts.get(1).priority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void happyFallbackPathParsesContentTextAsJson() {
        String responseJson = """
                {"content":[{"text":"{\\"cards\\":[\
                {\\"title\\":\\"T1\\",\\"description\\":\\"d\\",\\"priority\\":\\"LOW\\"},\
                {\\"title\\":\\"T2\\",\\"description\\":\\"d\\",\\"priority\\":\\"LOW\\"},\
                {\\"title\\":\\"T3\\",\\"description\\":\\"d\\",\\"priority\\":\\"LOW\\"},\
                {\\"title\\":\\"T4\\",\\"description\\":\\"d\\",\\"priority\\":\\"LOW\\"}]}"}]}
                """;

        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<CardDraft> drafts = service.breakdown("x", "en");

        assertThat(drafts).hasSize(4);
        assertThat(logAppender.list).anyMatch(e ->
                e.getLevel() == Level.WARN
                        && e.getFormattedMessage().toLowerCase().contains("falling back"));
    }

    @Test
    void fallbackStripsMarkdownFence() {
        // Patch K: model wraps structured output in ```json … ``` despite the system prompt.
        String responseJson = """
                {"content":[{"text":"```json\\n{\\"cards\\":[\
                {\\"title\\":\\"T1\\",\\"description\\":\\"d\\",\\"priority\\":\\"LOW\\"},\
                {\\"title\\":\\"T2\\",\\"description\\":\\"d\\",\\"priority\\":\\"LOW\\"},\
                {\\"title\\":\\"T3\\",\\"description\\":\\"d\\",\\"priority\\":\\"LOW\\"},\
                {\\"title\\":\\"T4\\",\\"description\\":\\"d\\",\\"priority\\":\\"LOW\\"}]}\\n```"}]}
                """;

        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<CardDraft> drafts = service.breakdown("x", "en");

        assertThat(drafts).hasSize(4);
    }

    @Test
    void bothInputAndTextPopulated_primaryWins() {
        // EC9 verified-safe: when both are present the primary path returns first — the text
        // fallback is never reached, so its "bogus" shape doesn't contaminate the result.
        String responseJson = """
                {"content":[{
                    "input":{"cards":[
                        {"title":"T1","description":"d","priority":"LOW"},
                        {"title":"T2","description":"d","priority":"LOW"},
                        {"title":"T3","description":"d","priority":"LOW"},
                        {"title":"T4","description":"d","priority":"LOW"}
                    ]},
                    "text":"this would not parse"
                }]}
                """;

        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<CardDraft> drafts = service.breakdown("x", "en");

        assertThat(drafts).hasSize(4);
        // No fallback WARN should fire — the primary path returned.
        assertThat(logAppender.list).noneMatch(e ->
                e.getFormattedMessage().toLowerCase().contains("falling back"));
    }

    // ------------------------------------------------------------------- upstream error paths

    @Test
    void upstream401LogsAuthFailedAndThrows503() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withUnauthorizedRequest()
                        .body("{\"error\":\"bad key\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.breakdown("x", "en"))
                .isInstanceOf(AiUnavailableException.class);

        assertThat(logAppender.list)
                .as("patch G: spec AC-required phrase \"Anthropic auth failed\"")
                .anyMatch(e -> e.getLevel() == Level.WARN
                        && e.getFormattedMessage().contains("Anthropic auth failed"));
    }

    @Test
    void upstream429ThrowsAiUnavailable() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body("rate limit hit")
                        .contentType(MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> service.breakdown("x", "en"))
                .isInstanceOf(AiUnavailableException.class);

        assertThat(logAppender.list).anyMatch(e ->
                e.getLevel() == Level.WARN
                        && e.getFormattedMessage().contains("Anthropic call failed"));
    }

    @Test
    void upstream503ThrowsAiUnavailable() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("upstream down")
                        .contentType(MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> service.breakdown("x", "en"))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void nonJsonResponseBodyThrowsAiUnavailable() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess("<html>502 Bad Gateway</html>", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> service.breakdown("x", "en"))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void emptyContentArrayThrowsAiUnavailable() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess("{\"content\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.breakdown("x", "en"))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("no cards payload");
    }

    @Test
    void non2xx400WithJsonBodyThrowsAiUnavailable() {
        // Edge: Anthropic sometimes returns 400 for malformed prompt payloads. Service treats
        // every non-2xx as 503 (no retry) and never surfaces the upstream status to the client.
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withBadRequest()
                        .body("{\"error\":\"bad payload\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.breakdown("x", "en"))
                .isInstanceOf(AiUnavailableException.class);
    }

    // ------------------------------------------------------------------- patch R: oversize body

    @Test
    void responseOver1MiBThrowsAiUnavailable() {
        // Patch R: generate a body just over 1 MiB and assert it's rejected before parsing.
        int size = (1 << 20) + 1024;
        String payload = "x".repeat(size);
        String body = "{\"content\":[{\"text\":\"" + payload + "\"}]}";

        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.breakdown("x", "en"))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("size limit");
    }

    // ------------------------------------------------------------------- VG9: key never logged

    @Test
    void apiKeyNeverAppearsInAnyLogLine() {
        // VG9: fire the 401 path (most likely to echo headers back) and assert no log line's
        // formatted message contains the unique marker embedded in the test's API key.
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withUnauthorizedRequest()
                        // Hostile case: upstream echoes the api-key header back in the body.
                        .body("{\"error\":\"invalid x-api-key " + API_KEY + "\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.breakdown("x", "en"))
                .isInstanceOf(AiUnavailableException.class);

        assertThat(logAppender.list)
                .as("patch B: no log line may contain the api-key marker, even when upstream echoes it")
                .noneMatch(e -> e.getFormattedMessage().contains("REDACTEDMARKER42"));
    }
}
