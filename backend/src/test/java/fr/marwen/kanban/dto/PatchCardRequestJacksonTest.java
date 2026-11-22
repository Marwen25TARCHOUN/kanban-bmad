package fr.marwen.kanban.dto;

import static org.assertj.core.api.Assertions.assertThat;

import fr.marwen.kanban.domain.CardStatus;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableJackson3Module;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pins the Jackson 3 deserialization contract for {@link PatchCardRequest} (AD-13).
 *
 * <p>{@code PatchCardRequest}'s canonical constructor coerces {@code null} component values to
 * {@code JsonNullable.undefined()} so {@code CardService.patch(...)} can call
 * {@code .isPresent()} on every field without an NPE. But that defensive coercion only matters
 * if Jackson actually hands a sparse PATCH body into that constructor with {@code null}s —
 * the alternative is that the library already deserializes absent keys as {@code undefined()}
 * directly, which is what we rely on. This test observes the library behavior so a future
 * module upgrade that changes the convention gets caught here instead of as a silent NPE in
 * {@code CardService}.
 */
class PatchCardRequestJacksonTest {

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JsonNullableJackson3Module())
            .build();

    @Test
    void sparseBodyDeserializesAbsentFieldsAsUndefined() throws Exception {
        PatchCardRequest req = mapper.readValue(
                "{\"title\":\"X\"}", PatchCardRequest.class);

        assertThat(req.title().isPresent()).as("present-with-value for the single key sent").isTrue();
        assertThat(req.title().get()).isEqualTo("X");

        assertThat(req.description().isUndefined()).as("description key absent -> undefined").isTrue();
        assertThat(req.priority().isUndefined()).as("priority key absent -> undefined").isTrue();
        assertThat(req.status().isUndefined()).as("status key absent -> undefined").isTrue();
        assertThat(req.position().isUndefined()).as("position key absent -> undefined").isTrue();
    }

    @Test
    void presentWithExplicitNullOnNullableFieldDeserializesAsPresentNull() throws Exception {
        PatchCardRequest req = mapper.readValue(
                "{\"description\":null}", PatchCardRequest.class);

        assertThat(req.description().isPresent())
                .as("explicit null on a nullable field -> present-with-null (AD-13 'clear')")
                .isTrue();
        assertThat(req.description().get()).isNull();
        assertThat(req.title().isUndefined()).isTrue();
    }

    @Test
    void presentWithValueOnStatusDeserializesEnumLiteral() throws Exception {
        PatchCardRequest req = mapper.readValue(
                "{\"status\":\"IN_PROGRESS\"}", PatchCardRequest.class);

        assertThat(req.status().isPresent()).isTrue();
        assertThat(req.status().get()).isEqualTo(CardStatus.IN_PROGRESS);
    }
}
