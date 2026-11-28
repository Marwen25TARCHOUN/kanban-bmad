package fr.marwen.kanban.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.marwen.kanban.KanbanApplication;
import fr.marwen.kanban.service.AiBreakdownService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice test for {@link ConfigController} — the AI-enabled runtime probe from AD-6 / CAP-7.
 *
 * <p>Patch F (review pass 1): derivation tightened to
 * {@code !context.getBeansOfType(AiBreakdownService.class).isEmpty()}. The slice-level nested
 * classes exercise the bean-presence branches with a {@link MockitoBean} toggle. The honesty
 * invariant — "property set without the bean actually loading produces {@code aiEnabled: false}"
 * — is pinned by real {@code @SpringBootTest} nested classes that exercise the SpEL gate from
 * patch A against the full bean graph. The previous slice-only version passed tautologically
 * because {@code @WebMvcTest} doesn't scan {@code @Service} beans regardless of properties.
 *
 * <p>{@link TestRestTemplate} is instantiated per-test (manual {@code new …}) because Spring
 * Boot 4's auto-config for it lives in {@code spring-boot-resttestclient} and isn't pulled in
 * by {@code @SpringBootTest} alone.
 */
class ConfigControllerTest {

    @Nested
    @WebMvcTest(ConfigController.class)
    class WhenAiServiceBeanPresent {

        @Autowired
        MockMvc mockMvc;

        @MockitoBean(name = "aiBreakdownService")
        AiBreakdownService aiBreakdownService;

        @Test
        void returnsAiEnabledTrue() throws Exception {
            mockMvc.perform(get("/api/config"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.aiEnabled").value(true))
                    .andExpect(jsonPath("$.locales[0]").value("fr"))
                    .andExpect(jsonPath("$.locales[1]").value("en"));
        }
    }

    @Nested
    @WebMvcTest(ConfigController.class)
    class WhenAiServiceBeanAbsent {

        @Autowired
        MockMvc mockMvc;

        @Test
        void returnsAiEnabledFalse() throws Exception {
            mockMvc.perform(get("/api/config"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.aiEnabled").value(false))
                    .andExpect(jsonPath("$.locales[0]").value("fr"))
                    .andExpect(jsonPath("$.locales[1]").value("en"));
        }
    }

    /**
     * Patch F part 2 (review pass 1): a REAL {@code @SpringBootTest} exercising the full bean
     * registration path. No {@code anthropic.api-key} set → AI beans never load →
     * {@code aiEnabled: false}. The old slice-only {@code WhenPropertySetButBeanAbsent} fixture
     * was tautological; this class proves CAP-7 end-to-end against the SpEL gate from patch A.
     */
    @Nested
    @SpringBootTest(
            classes = KanbanApplication.class,
            webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    class HonestyInvariantIntegrationIT {

        @LocalServerPort
        int port;

        @Test
        void keyAbsentReportsAiEnabledFalse() {
            ResponseEntity<Config> response = new TestRestTemplate()
                    .getForEntity("http://localhost:" + port + "/api/config", Config.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().aiEnabled())
                    .as("absent key => bean graph absent => aiEnabled false")
                    .isFalse();
            assertThat(response.getBody().locales()).containsExactly("fr", "en");
        }
    }

    /**
     * Patch A invariant: property set to a non-blank value DOES load the AI bean graph and
     * {@code aiEnabled} flips to true. Complements the absent / blank branches.
     * Patch C: non-Anthropic-shaped placeholder.
     */
    @Nested
    @SpringBootTest(
            classes = KanbanApplication.class,
            webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @TestPropertySource(properties = "anthropic.api-key=test-key-0001")
    class KeyPresentIntegrationIT {

        @LocalServerPort
        int port;

        @Test
        void keyPresentReportsAiEnabledTrue() {
            ResponseEntity<Config> response = new TestRestTemplate()
                    .getForEntity("http://localhost:" + port + "/api/config", Config.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().aiEnabled())
                    .as("non-blank key => bean graph loads => aiEnabled true")
                    .isTrue();
        }
    }

    /**
     * Patch A invariant: property set to the EMPTY STRING must behave like absent — the SpEL
     * gate's {@code .trim() != ''} rejects it, the AI bean graph stays absent, and
     * {@code aiEnabled} stays false. This is the honesty-invariant regression test for the
     * highest-severity review finding.
     */
    @Nested
    @SpringBootTest(
            classes = KanbanApplication.class,
            webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @TestPropertySource(properties = "anthropic.api-key=")
    class KeyBlankIntegrationIT {

        @LocalServerPort
        int port;

        @Test
        void blankKeyReportsAiEnabledFalse() {
            ResponseEntity<Config> response = new TestRestTemplate()
                    .getForEntity("http://localhost:" + port + "/api/config", Config.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().aiEnabled())
                    .as("blank key must not load the bean graph (patch A honesty)")
                    .isFalse();
        }
    }

    /** Patch A: whitespace-only key also fails the SpEL gate thanks to {@code .trim() != ''}. */
    @Nested
    @SpringBootTest(
            classes = KanbanApplication.class,
            webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @TestPropertySource(properties = "anthropic.api-key=   ")
    class KeyWhitespaceOnlyIntegrationIT {

        @LocalServerPort
        int port;

        @Test
        void whitespaceKeyReportsAiEnabledFalse() {
            ResponseEntity<Config> response = new TestRestTemplate()
                    .getForEntity("http://localhost:" + port + "/api/config", Config.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().aiEnabled()).isFalse();
        }
    }

    /** Lightweight DTO for the integration asserts (mirrors {@code ConfigController.ConfigResponse}). */
    record Config(boolean aiEnabled, java.util.List<String> locales) {
    }
}
