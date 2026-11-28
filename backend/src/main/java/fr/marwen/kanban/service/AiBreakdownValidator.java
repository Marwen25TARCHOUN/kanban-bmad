package fr.marwen.kanban.service;

import fr.marwen.kanban.config.AnthropicRestClientConfig;
import fr.marwen.kanban.domain.Priority;
import fr.marwen.kanban.dto.BreakdownResponse.CardDraft;
import fr.marwen.kanban.exception.AiUnavailableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Defense-in-depth validator for the Anthropic structured-output response (AD-5 revised).
 *
 * <p>Structured output is a best-effort upstream signal: the Anthropic Messages API accepts the
 * schema in {@code output_config.format} and nudges the model toward it, but we don't trust that
 * format drift won't land one day. This validator re-checks every invariant the schema asserts
 * — count ∈ {@code [4, 6]}, non-blank/trimmed/≤120-char title, non-blank/trimmed/≤2000-char
 * description, priority ∈ {@link Priority} — and throws {@link AiUnavailableException} on the
 * first violation with a reason string the WARN log captures (per AD-5). The 503 path stays
 * deterministic regardless of upstream contract changes.
 *
 * <p>Input is the already-extracted {@code cards} array {@link JsonNode}, not the full response
 * envelope. The service owns the envelope-path extraction; this bean owns rule enforcement.
 */
@Component
@ConditionalOnExpression(AnthropicRestClientConfig.KEY_PRESENT_EXPRESSION)
public class AiBreakdownValidator {

    static final int MIN_CARDS = 4;
    static final int MAX_CARDS = 6;
    static final int MAX_TITLE_LENGTH = 120;
    static final int MAX_DESCRIPTION_LENGTH = 2000;

    /**
     * Re-check the Anthropic breakdown payload card-by-card. Returns the validated list of
     * {@link CardDraft}s; throws on the first rule violation.
     *
     * @param cardsNode the extracted {@code cards} array node (never {@code null} per contract —
     *     the service surfaces a missing node as {@link AiUnavailableException} before calling)
     * @throws AiUnavailableException with reason prefixed {@code "validator: "}
     */
    public List<CardDraft> validate(JsonNode cardsNode) {
        if (cardsNode == null || cardsNode.isNull() || cardsNode.isMissingNode()) {
            throw new AiUnavailableException("validator: cards node is missing");
        }
        if (!cardsNode.isArray()) {
            throw new AiUnavailableException("validator: cards is not an array");
        }
        int count = cardsNode.size();
        if (count < MIN_CARDS || count > MAX_CARDS) {
            throw new AiUnavailableException(
                    "validator: card count " + count + " outside [" + MIN_CARDS + "," + MAX_CARDS + "]");
        }

        List<CardDraft> drafts = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            JsonNode card = cardsNode.get(i);
            if (card == null || !card.isObject()) {
                throw new AiUnavailableException("validator: card[" + i + "] is not an object");
            }
            drafts.add(toDraft(card, i));
        }
        return drafts;
    }

    private CardDraft toDraft(JsonNode card, int index) {
        String title = requireStringField(card, "title", index, MAX_TITLE_LENGTH);
        String description = requireStringField(card, "description", index, MAX_DESCRIPTION_LENGTH);
        Priority priority = requirePriority(card, index);
        return new CardDraft(title, description, priority);
    }

    private String requireStringField(JsonNode card, String field, int index, int maxLength) {
        JsonNode node = card.get(field);
        if (node == null || node.isNull() || node.isMissingNode()) {
            throw new AiUnavailableException(
                    "validator: card[" + index + "] missing \"" + field + "\"");
        }
        if (!node.isTextual()) {
            throw new AiUnavailableException(
                    "validator: card[" + index + "] \"" + field + "\" is not a string");
        }
        // Patch N: strip() handles standard whitespace (every Character.isWhitespace code
        // point). The extra \p{Zs}\p{Cf} replace catches NBSP (U+00A0), ZWSP (U+200B), ZWJ /
        // ZWNJ, BOM — characters Java intentionally excludes from isWhitespace() because they
        // aren't layout whitespace. A title that's visually empty but non-isWhitespace would
        // otherwise slip through. The returned string is the stripped form (invisible chars
        // preserved only when the title has real content beside them).
        String stripped = node.asText().strip();
        String visible = stripped.replaceAll("[\\p{Zs}\\p{Cf}]+", "");
        if (visible.isEmpty()) {
            throw new AiUnavailableException(
                    "validator: card[" + index + "] \"" + field + "\" is blank");
        }
        if (stripped.length() > maxLength) {
            throw new AiUnavailableException(
                    "validator: card[" + index + "] \"" + field + "\" length "
                            + stripped.length() + " > " + maxLength);
        }
        return stripped;
    }

    private Priority requirePriority(JsonNode card, int index) {
        JsonNode node = card.get("priority");
        if (node == null || node.isNull() || node.isMissingNode()) {
            throw new AiUnavailableException(
                    "validator: card[" + index + "] missing \"priority\"");
        }
        if (!node.isTextual()) {
            throw new AiUnavailableException(
                    "validator: card[" + index + "] \"priority\" is not a string");
        }
        String raw = node.asText();
        try {
            // Patch L: upper-case the raw value so a model drift to "high" / "Medium" still
            // validates. Defense-in-depth: the schema constrains to UPPER_SNAKE, but the
            // validator is where we assert that invariant post-flight.
            return Priority.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AiUnavailableException(
                    "validator: card[" + index + "] \"priority\" \"" + raw
                            + "\" not in {LOW,MEDIUM,HIGH}",
                    ex);
        }
    }
}
