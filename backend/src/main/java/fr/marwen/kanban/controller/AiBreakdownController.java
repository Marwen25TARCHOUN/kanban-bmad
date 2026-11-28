package fr.marwen.kanban.controller;

import fr.marwen.kanban.config.AnthropicRestClientConfig;
import fr.marwen.kanban.dto.BreakdownRequest;
import fr.marwen.kanban.dto.BreakdownResponse;
import fr.marwen.kanban.service.AiBreakdownService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP surface for the flagship AI-breakdown feature (CAP-2; story 5) — gate #3 of the AI bean
 * graph (AD-5 revised, AD-6).
 *
 * <p>Thin routing shell per AD-1: Jakarta validates the {@link BreakdownRequest}, delegate the
 * work to {@link AiBreakdownService}, wrap the returned drafts in {@link BreakdownResponse}. No
 * try/catch here — {@code GlobalExceptionHandler} maps 400 ({@code MethodArgumentNotValidException})
 * and 503 ({@code AiUnavailableException}) centrally (AD-2).
 *
 * <p>When {@code anthropic.api-key} is absent the controller is not instantiated at all — no
 * bean, no handler mapping, so {@code /api/ai/breakdown} naturally returns 404. That 404 is the
 * CAP-7 graceful-degradation signal the frontend reads from {@code /api/config.aiEnabled} at
 * bootstrap; a direct hit on the endpoint in that state is a client error that doesn't need a
 * custom "AI disabled" branch.
 */
@RestController
@RequestMapping("/api/ai")
@ConditionalOnExpression(AnthropicRestClientConfig.KEY_PRESENT_EXPRESSION)
public class AiBreakdownController {

    private final AiBreakdownService service;

    public AiBreakdownController(AiBreakdownService service) {
        this.service = service;
    }

    @PostMapping("/breakdown")
    public BreakdownResponse breakdown(@Valid @RequestBody BreakdownRequest req) {
        return new BreakdownResponse(service.breakdown(req.idea(), req.locale()));
    }
}
