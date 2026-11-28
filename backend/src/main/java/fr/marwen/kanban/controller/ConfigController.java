package fr.marwen.kanban.controller;

import fr.marwen.kanban.service.AiBreakdownService;
import java.util.List;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Frontend-facing config probe (architecture AD-6 / CAP-7).
 *
 * <p>The frontend reads {@code /api/config} at bootstrap and renders (or hides) the AI breakdown
 * bar based on {@code aiEnabled}. Story 5 tightened this from "read {@code anthropic.api-key}
 * property" to "check the AI service bean is present in the context": the entire AI bean graph
 * is gated by {@code @ConditionalOnProperty(name="anthropic.api-key", matchIfMissing=false)}, so
 * bean presence IS the ground truth about whether {@code POST /api/ai/breakdown} would succeed.
 * A stale or oddly-formatted property that fails to register the beans will now report
 * {@code aiEnabled: false}, matching what the client would experience.
 *
 * <p>The {@link ApplicationContext#containsBean(String)} call is a cheap in-memory lookup; the
 * conditional-bean evaluation has long settled by the time a request reaches this controller
 * (Spring finishes bean registration before HTTP binding).
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private static final List<String> LOCALES = List.of("fr", "en");

    private final ApplicationContext context;

    public ConfigController(ApplicationContext context) {
        this.context = context;
    }

    @GetMapping
    public ConfigResponse get() {
        // Patch F: type-based lookup — a future rename of AiBreakdownService fails at COMPILE
        // time instead of silently reporting `aiEnabled: false` forever. The class is always on
        // the classpath; the bean is present only when the SpEL gate in
        // AnthropicRestClientConfig.KEY_PRESENT_EXPRESSION matches.
        boolean aiEnabled = !context.getBeansOfType(AiBreakdownService.class).isEmpty();
        return new ConfigResponse(aiEnabled, LOCALES);
    }

    /**
     * Nested record keeps the payload record collocated with its single consumer — one file for
     * the controller + its response shape, no extra {@code dto/} noise for a two-field DTO.
     */
    public record ConfigResponse(boolean aiEnabled, List<String> locales) {
    }
}
