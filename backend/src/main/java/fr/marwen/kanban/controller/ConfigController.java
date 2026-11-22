package fr.marwen.kanban.controller;

import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Frontend-facing config probe (architecture AD-6 / CAP-7).
 *
 * <p>The frontend reads {@code /api/config} at bootstrap and renders (or hides) the AI breakdown
 * bar based on {@code aiEnabled}. The flag is derived from Spring's {@link Environment} so the
 * same property-source ladder covers every mode: the {@code ANTHROPIC_API_KEY} env var is picked
 * up via Spring's relaxed binding (env var → {@code anthropic.api-key} property), {@code .env}
 * passthrough in Compose lands at the same key, and tests can swap in {@code @TestPropertySource}
 * without touching {@code System.getenv} (AD-6 "never a direct {@code System.getenv}").
 *
 * <p>Story 2 does NOT gate beans on this property — that's story 5's job when
 * {@code AiBreakdownController} actually exists. Here we just expose the flag.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private static final List<String> LOCALES = List.of("fr", "en");

    private final Environment environment;

    public ConfigController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping
    public ConfigResponse get() {
        String apiKey = environment.getProperty("anthropic.api-key");
        boolean aiEnabled = apiKey != null && !apiKey.isBlank();
        return new ConfigResponse(aiEnabled, LOCALES);
    }

    /**
     * Nested record keeps the payload record collocated with its single consumer — one file for
     * the controller + its response shape, no extra {@code dto/} noise for a two-field DTO.
     */
    public record ConfigResponse(boolean aiEnabled, List<String> locales) {
    }
}
