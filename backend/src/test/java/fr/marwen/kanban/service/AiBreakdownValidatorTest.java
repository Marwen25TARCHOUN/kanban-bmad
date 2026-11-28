package fr.marwen.kanban.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.marwen.kanban.domain.Priority;
import fr.marwen.kanban.dto.BreakdownResponse.CardDraft;
import fr.marwen.kanban.exception.AiUnavailableException;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pure unit tests for {@link AiBreakdownValidator} — covers every "Anthropic returns …" row of
 * the story-5 I/O matrix. No Spring context; the component is a plain POJO.
 *
 * <p>Twelve named cases map 1:1 onto the matrix: three happy counts (4, 5, 6), both out-of-range
 * counts (3, 7), three missing-field cases (title, description, priority), blank/whitespace
 * title, two over-length cases (title 121, description 2001), invalid priority enum, and the
 * "non-JSON" row via a malformed {@link JsonNode} (non-array root).
 */
class AiBreakdownValidatorTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final AiBreakdownValidator validator = new AiBreakdownValidator();

    // ------------------------------------------------------------------- happy paths

    @Test
    void acceptsMinimumFourCards() throws Exception {
        JsonNode cards = cardsArray(4);

        List<CardDraft> drafts = validator.validate(cards);

        assertThat(drafts).hasSize(4);
        assertThat(drafts.get(0).title()).isEqualTo("Card 0");
        assertThat(drafts.get(0).priority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void acceptsFiveCards() throws Exception {
        JsonNode cards = cardsArray(5);

        List<CardDraft> drafts = validator.validate(cards);

        assertThat(drafts).hasSize(5);
    }

    @Test
    void acceptsMaximumSixCards() throws Exception {
        JsonNode cards = cardsArray(6);

        List<CardDraft> drafts = validator.validate(cards);

        assertThat(drafts).hasSize(6);
        assertThat(drafts).allSatisfy(d -> {
            assertThat(d.title()).isNotBlank();
            assertThat(d.description()).isNotBlank();
            assertThat(d.priority()).isIn(Priority.LOW, Priority.MEDIUM, Priority.HIGH);
        });
    }

    // ------------------------------------------------------------------- count out of range

    @Test
    void rejectsThreeCardsAsCountTooLow() throws Exception {
        JsonNode cards = cardsArray(3);

        assertThatThrownBy(() -> validator.validate(cards))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("validator:")
                .hasMessageContaining("count 3");
    }

    @Test
    void rejectsSevenCardsAsCountTooHigh() throws Exception {
        JsonNode cards = cardsArray(7);

        assertThatThrownBy(() -> validator.validate(cards))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("validator:")
                .hasMessageContaining("count 7");
    }

    // ------------------------------------------------------------------- missing fields

    @Test
    void rejectsMissingTitleKey() throws Exception {
        String json = """
                [
                  {"description":"d","priority":"LOW"},
                  {"title":"T2","description":"d","priority":"LOW"},
                  {"title":"T3","description":"d","priority":"LOW"},
                  {"title":"T4","description":"d","priority":"LOW"}
                ]
                """;
        JsonNode cards = mapper.readTree(json);

        assertThatThrownBy(() -> validator.validate(cards))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("missing \"title\"");
    }

    @Test
    void rejectsMissingDescriptionKey() throws Exception {
        String json = """
                [
                  {"title":"T1","priority":"LOW"},
                  {"title":"T2","description":"d","priority":"LOW"},
                  {"title":"T3","description":"d","priority":"LOW"},
                  {"title":"T4","description":"d","priority":"LOW"}
                ]
                """;
        JsonNode cards = mapper.readTree(json);

        assertThatThrownBy(() -> validator.validate(cards))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("missing \"description\"");
    }

    @Test
    void rejectsMissingPriorityKey() throws Exception {
        String json = """
                [
                  {"title":"T1","description":"d"},
                  {"title":"T2","description":"d","priority":"LOW"},
                  {"title":"T3","description":"d","priority":"LOW"},
                  {"title":"T4","description":"d","priority":"LOW"}
                ]
                """;
        JsonNode cards = mapper.readTree(json);

        assertThatThrownBy(() -> validator.validate(cards))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("missing \"priority\"");
    }

    // ------------------------------------------------------------------- blank / whitespace

    @Test
    void rejectsEmptyTitle() throws Exception {
        String json = """
                [
                  {"title":"","description":"d","priority":"LOW"},
                  {"title":"T2","description":"d","priority":"LOW"},
                  {"title":"T3","description":"d","priority":"LOW"},
                  {"title":"T4","description":"d","priority":"LOW"}
                ]
                """;
        JsonNode cards = mapper.readTree(json);

        assertThatThrownBy(() -> validator.validate(cards))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("\"title\" is blank");
    }

    @Test
    void rejectsWhitespaceOnlyTitle() throws Exception {
        String json = """
                [
                  {"title":"   ","description":"d","priority":"LOW"},
                  {"title":"T2","description":"d","priority":"LOW"},
                  {"title":"T3","description":"d","priority":"LOW"},
                  {"title":"T4","description":"d","priority":"LOW"}
                ]
                """;
        JsonNode cards = mapper.readTree(json);

        assertThatThrownBy(() -> validator.validate(cards))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("\"title\" is blank");
    }

    // ------------------------------------------------------------------- over-length

    @Test
    void rejectsTitleOver120Characters() throws Exception {
        String longTitle = "T".repeat(121);
        String json = """
                [
                  {"title":"%s","description":"d","priority":"LOW"},
                  {"title":"T2","description":"d","priority":"LOW"},
                  {"title":"T3","description":"d","priority":"LOW"},
                  {"title":"T4","description":"d","priority":"LOW"}
                ]
                """.formatted(longTitle);
        JsonNode cards = mapper.readTree(json);

        assertThatThrownBy(() -> validator.validate(cards))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("\"title\" length 121");
    }

    @Test
    void rejectsDescriptionOver2000Characters() throws Exception {
        String longDescription = "d".repeat(2001);
        String json = """
                [
                  {"title":"T1","description":"%s","priority":"LOW"},
                  {"title":"T2","description":"d","priority":"LOW"},
                  {"title":"T3","description":"d","priority":"LOW"},
                  {"title":"T4","description":"d","priority":"LOW"}
                ]
                """.formatted(longDescription);
        JsonNode cards = mapper.readTree(json);

        assertThatThrownBy(() -> validator.validate(cards))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("\"description\" length 2001");
    }

    // ------------------------------------------------------------------- case-insensitive (patch L)

    @Test
    void acceptsLowercasePriorityDrift() throws Exception {
        // Patch L: `Priority.valueOf(raw.toUpperCase(Locale.ROOT))` lets a model drift to
        // "high" / "Medium" survive the validator. Defense-in-depth per the enum invariant.
        String json = """
                [
                  {"title":"T1","description":"d","priority":"high"},
                  {"title":"T2","description":"d","priority":"Medium"},
                  {"title":"T3","description":"d","priority":"LOW"},
                  {"title":"T4","description":"d","priority":"LOW"}
                ]
                """;
        JsonNode cards = mapper.readTree(json);

        List<CardDraft> drafts = validator.validate(cards);

        assertThat(drafts).extracting(CardDraft::priority)
                .containsExactly(Priority.HIGH, Priority.MEDIUM, Priority.LOW, Priority.LOW);
    }

    // ------------------------------------------------------------------- Unicode-aware blank (patch N)

    @Test
    void rejectsNonBreakingSpaceOnlyTitleAsBlank() throws Exception {
        // Patch N: strip() + isBlank() catch NBSP (U+00A0). The pre-patch trim() + isEmpty()
        // would have let this slip through because trim() only strips <=U+0020.
        String json = """
                [
                  {"title":"  ","description":"d","priority":"LOW"},
                  {"title":"T2","description":"d","priority":"LOW"},
                  {"title":"T3","description":"d","priority":"LOW"},
                  {"title":"T4","description":"d","priority":"LOW"}
                ]
                """;
        JsonNode cards = mapper.readTree(json);

        assertThatThrownBy(() -> validator.validate(cards))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("\"title\" is blank");
    }

    // ------------------------------------------------------------------- invalid enum

    @Test
    void rejectsPriorityOutsideEnum() throws Exception {
        String json = """
                [
                  {"title":"T1","description":"d","priority":"URGENT"},
                  {"title":"T2","description":"d","priority":"LOW"},
                  {"title":"T3","description":"d","priority":"LOW"},
                  {"title":"T4","description":"d","priority":"LOW"}
                ]
                """;
        JsonNode cards = mapper.readTree(json);

        assertThatThrownBy(() -> validator.validate(cards))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("\"URGENT\"")
                .hasMessageContaining("{LOW,MEDIUM,HIGH}");
    }

    // ------------------------------------------------------------------- non-array / non-JSON

    @Test
    void rejectsNonArrayNode() throws Exception {
        // The service's envelope parser normally lands an array in here; if structured output
        // drifted to an object, we reject hard with a clear reason — this is the "non-JSON" row
        // of the I/O matrix (the input IS parsed JSON, just not the shape the service promises).
        JsonNode objectRoot = mapper.readTree("{\"cards\": 42}").get("cards");

        assertThatThrownBy(() -> validator.validate(objectRoot))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("not an array");
    }

    @Test
    void rejectsNullNode() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void rejectsCardThatIsNotAnObject() throws Exception {
        String json = """
                [
                  "not-a-card",
                  {"title":"T2","description":"d","priority":"LOW"},
                  {"title":"T3","description":"d","priority":"LOW"},
                  {"title":"T4","description":"d","priority":"LOW"}
                ]
                """;
        JsonNode cards = mapper.readTree(json);

        assertThatThrownBy(() -> validator.validate(cards))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("card[0] is not an object");
    }

    // ------------------------------------------------------------------- helpers

    private JsonNode cardsArray(int count) throws Exception {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"title\":\"Card ").append(i).append("\",")
                    .append("\"description\":\"Desc ").append(i).append("\",")
                    .append("\"priority\":\"MEDIUM\"}");
        }
        sb.append(']');
        return mapper.readTree(sb.toString());
    }
}
