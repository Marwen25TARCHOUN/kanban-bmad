package fr.marwen.kanban.controller;

import static org.assertj.core.api.Assertions.assertThat;

import fr.marwen.kanban.KanbanApplication;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Patch I (review pass 1) — CAP-7 contract test: when {@code anthropic.api-key} is absent (or
 * blank, per patch A's SpEL gate), POST to {@code /api/ai/breakdown} must return 404 because
 * the controller bean never loaded. This is the sibling to {@code ConfigController} reporting
 * {@code aiEnabled: false} — together they're the "AI disabled" signal the frontend reads.
 *
 * <p>Full {@code @SpringBootTest} with a random port so the real Spring Boot bean registration
 * runs against our SpEL gates. A {@code @WebMvcTest(AiBreakdownController.class)} wouldn't catch
 * this — the slice loads the named controller regardless of conditions.
 *
 * <p>{@link TestRestTemplate} is instantiated per-test (no autowire) because Spring Boot 4's
 * {@code TestRestTemplate} auto-config lives in {@code spring-boot-resttestclient} and isn't
 * pulled in by {@code @SpringBootTest} alone — a manual {@code new TestRestTemplate()} is
 * cleaner than adding {@code @AutoConfigureTestRestTemplate} just for one call.
 */
@SpringBootTest(
        classes = KanbanApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiBreakdownEndpointAbsenceTest {

    @LocalServerPort
    int port;

    @Test
    void postAiBreakdownReturns404WhenApiKeyAbsent() {
        TestRestTemplate rest = new TestRestTemplate();
        ResponseEntity<String> response = rest.postForEntity(
                "http://localhost:" + port + "/api/ai/breakdown",
                Map.of("idea", "x", "locale", "en"),
                String.class);

        assertThat(response.getStatusCode())
                .as("CAP-7: no `anthropic.api-key` => no controller mapping => 404")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
