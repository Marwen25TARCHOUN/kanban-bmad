package fr.marwen.kanban.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice test for {@link ConfigController} — the AI-enabled runtime probe from AD-6 / CAP-7.
 *
 * <p>Two nested classes pin the on/off branches of {@code aiEnabled} via
 * {@code @TestPropertySource} — this is the deliberate injection point so no real env var reach
 * is needed and the service stays side-effect free.
 */
class ConfigControllerTest {

    @Nested
    @WebMvcTest(ConfigController.class)
    @TestPropertySource(properties = "anthropic.api-key=sk-ant-xxx")
    class WhenApiKeyPresent {

        @Autowired
        MockMvc mockMvc;

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
    @TestPropertySource(properties = "anthropic.api-key=")
    class WhenApiKeyBlank {

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
     * Covers the case Compose ships: no {@code .env} file, no exported env var, the property
     * is never set at all. {@code Environment.getProperty("anthropic.api-key")} returns
     * {@code null} here — distinct from the blank string above — and the handler's
     * {@code != null && !isBlank()} must treat it the same way.
     */
    @Nested
    @WebMvcTest(ConfigController.class)
    class WhenApiKeyAbsent {

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
}
