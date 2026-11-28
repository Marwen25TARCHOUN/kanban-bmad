package fr.marwen.kanban.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.marwen.kanban.domain.Priority;
import fr.marwen.kanban.dto.BreakdownResponse.CardDraft;
import fr.marwen.kanban.exception.AiUnavailableException;
import fr.marwen.kanban.exception.GlobalExceptionHandler;
import fr.marwen.kanban.service.AiBreakdownService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice test for {@link AiBreakdownController} — exercises the AD-2 hard split between 400
 * (pre-call Jakarta validation) and 503 ({@link AiUnavailableException} from the service).
 *
 * <p>{@code anthropic.api-key} is set to a non-blank placeholder so the controller's SpEL gate
 * triggers; the service dependency is mocked via {@link MockitoBean}, so no real Anthropic call
 * happens. Covers (patch H) EN and FR microcopy paths — every assertion against a localized
 * string sets {@code Accept-Language} explicitly rather than relying on the JVM default.
 *
 * <p>Patch C: the placeholder is {@code test-key-0001}, deliberately not Anthropic-shaped, so
 * the AC grep over {@code backend/} for real-key prefixes returns zero matches.
 */
@WebMvcTest(AiBreakdownController.class)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "anthropic.api-key=test-key-0001")
class AiBreakdownControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AiBreakdownService aiBreakdownService;

    // ------------------------------------------------------------------- 200 happy

    @Test
    void happyPathReturns200WithFiveCards() throws Exception {
        when(aiBreakdownService.breakdown(eq("Deploy to Fly.io"), eq("en")))
                .thenReturn(List.of(
                        new CardDraft("Set up Fly.io account and CLI",
                                "Install flyctl, authenticate, verify free-tier access.",
                                Priority.MEDIUM),
                        new CardDraft("Write Dockerfile + .dockerignore",
                                "Multi-stage build, non-root user, slim base image.",
                                Priority.HIGH),
                        new CardDraft("Configure fly.toml",
                                "Primary region, health checks, autoscaling, env vars.",
                                Priority.MEDIUM),
                        new CardDraft("Test deploy and rollback",
                                "Deploy, verify response, trigger rollback, confirm reversal.",
                                Priority.MEDIUM),
                        new CardDraft("Document deploy process",
                                "Short README section with the day-to-day commands.",
                                Priority.LOW)));

        mockMvc.perform(post("/api/ai/breakdown")
                        .header("Accept-Language", "en")
                        .contentType("application/json")
                        .content("""
                                {"idea":"Deploy to Fly.io","locale":"en"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards").isArray())
                .andExpect(jsonPath("$.cards.length()").value(5))
                .andExpect(jsonPath("$.cards[0].title").value("Set up Fly.io account and CLI"))
                .andExpect(jsonPath("$.cards[0].priority").value("MEDIUM"))
                .andExpect(jsonPath("$.cards[1].priority").value("HIGH"))
                .andExpect(jsonPath("$.cards[4].priority").value("LOW"));
    }

    // ------------------------------------------------------------------- 400 paths (EN)

    @Test
    void emptyIdeaReturns400ValidationFailedEn() throws Exception {
        mockMvc.perform(post("/api/ai/breakdown")
                        .header("Accept-Language", "en")
                        .contentType("application/json")
                        .content("""
                                {"idea":"","locale":"en"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("idea")));

        verify(aiBreakdownService, never()).breakdown(any(), any());
    }

    @Test
    void invalidLocaleReturns400ValidationFailedEn() throws Exception {
        mockMvc.perform(post("/api/ai/breakdown")
                        .header("Accept-Language", "en")
                        .contentType("application/json")
                        .content("""
                                {"idea":"Deploy somewhere","locale":"de"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("locale")));

        verify(aiBreakdownService, never()).breakdown(any(), any());
    }

    /** Patch J — idea > 500 chars is rejected pre-call with the {@code @Size} message. */
    @Test
    void ideaOver500CharsReturns400SizeMessage() throws Exception {
        String longIdea = "x".repeat(501);

        mockMvc.perform(post("/api/ai/breakdown")
                        .header("Accept-Language", "en")
                        .contentType("application/json")
                        .content("""
                                {"idea":"%s","locale":"en"}
                                """.formatted(longIdea)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("between 1 and 500")));

        verify(aiBreakdownService, never()).breakdown(any(), any());
    }

    /** Patch O — a control character in the idea is rejected 400 (not 503). */
    @Test
    void controlCharInIdeaReturns400NotLeakingTo503() throws Exception {
        mockMvc.perform(post("/api/ai/breakdown")
                        .header("Accept-Language", "en")
                        .contentType("application/json")
                        .content("{\"idea\":\"Deploy\\u0000app\",\"locale\":\"en\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("control characters")));

        verify(aiBreakdownService, never()).breakdown(any(), any());
    }

    // ------------------------------------------------------------------- 503 path (EN)

    @Test
    void serviceThrowingAiUnavailableReturns503EnvelopeEn() throws Exception {
        when(aiBreakdownService.breakdown(eq("Deploy to Fly.io"), eq("en")))
                .thenThrow(new AiUnavailableException("validator: card count 3 outside [4,6]"));

        mockMvc.perform(post("/api/ai/breakdown")
                        .header("Accept-Language", "en")
                        .contentType("application/json")
                        .content("""
                                {"idea":"Deploy to Fly.io","locale":"en"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("AI_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("AI service unavailable, try again"));
    }

    // ------------------------------------------------------------------- 400 / 503 FR variants (patch H)

    @Test
    void emptyIdeaReturns400ValidationFailedFr() throws Exception {
        mockMvc.perform(post("/api/ai/breakdown")
                        .header("Accept-Language", "fr")
                        .contentType("application/json")
                        .content("""
                                {"idea":"","locale":"fr"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("ne doit pas être vide")));
    }

    @Test
    void invalidLocaleReturns400ValidationFailedFr() throws Exception {
        // Hibernate Validator ships its own French translation of @Pattern — the raw message
        // reaches GlobalExceptionHandler already localized as "doit correspondre à \"fr|en\"",
        // bypassing the EN-to-FR map. Asserting the Hibernate FR phrase is the honest check.
        mockMvc.perform(post("/api/ai/breakdown")
                        .header("Accept-Language", "fr")
                        .contentType("application/json")
                        .content("""
                                {"idea":"Deployer quelque part","locale":"de"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("doit correspondre")));
    }

    @Test
    void serviceThrowingAiUnavailableReturns503EnvelopeFr() throws Exception {
        when(aiBreakdownService.breakdown(eq("Déployer sur Fly.io"), eq("fr")))
                .thenThrow(new AiUnavailableException("validator: card count 3 outside [4,6]"));

        mockMvc.perform(post("/api/ai/breakdown")
                        .header("Accept-Language", "fr")
                        .contentType("application/json")
                        .content("""
                                {"idea":"Déployer sur Fly.io","locale":"fr"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("AI_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Service IA indisponible, réessayez"));
    }
}
