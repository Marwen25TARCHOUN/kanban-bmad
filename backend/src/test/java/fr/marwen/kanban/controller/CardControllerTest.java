package fr.marwen.kanban.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.marwen.kanban.config.CorsConfig;
import fr.marwen.kanban.domain.CardStatus;
import fr.marwen.kanban.domain.Priority;
import fr.marwen.kanban.dto.BoardPartitionsResponse;
import fr.marwen.kanban.dto.CardDto;
import fr.marwen.kanban.dto.CreateBatchRequest;
import fr.marwen.kanban.dto.CreateCardRequest;
import fr.marwen.kanban.dto.PatchCardRequest;
import fr.marwen.kanban.exception.GlobalExceptionHandler;
import fr.marwen.kanban.service.CardService;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice test for {@link CardController} — happy path per endpoint plus at least one
 * validation-failure path (architecture AD-11; story 2 Boundaries).
 *
 * <p>Imports {@link GlobalExceptionHandler} so 400 / 404 envelopes come through the real advice,
 * and {@link CorsConfig} so {@code JsonNullableModule} is registered on Jackson (without it,
 * {@code PatchCardRequest} fields fail to deserialize).
 */
@WebMvcTest(CardController.class)
@Import({GlobalExceptionHandler.class, CorsConfig.class})
class CardControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CardService cardService;

    // ------------------------------------------------------------------- GET /api/cards

    @Test
    void listReturnsOkWithJsonArray() throws Exception {
        when(cardService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // ------------------------------------------------------------------- POST /api/cards

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        UUID id = UUID.randomUUID();
        when(cardService.create(any(CreateCardRequest.class))).thenReturn(sampleDto(id));

        mockMvc.perform(post("/api/cards")
                        .contentType("application/json")
                        .content("""
                                {"title":"Ship it","priority":"MEDIUM"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/cards/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.aiGenerated").value(false));
    }

    @Test
    void createWithMissingTitleReturns400WithEnvelope() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .contentType("application/json")
                        .content("""
                                {"priority":"MEDIUM"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("title")));
    }

    @Test
    void createDropsClientSuppliedServerOwnedFieldsSilently() throws Exception {
        UUID id = UUID.randomUUID();
        // Service always returns its server-owned defaults regardless of what the client sent.
        when(cardService.create(any(CreateCardRequest.class))).thenReturn(sampleDto(id));

        mockMvc.perform(post("/api/cards")
                        .contentType("application/json")
                        .content("""
                                {"id":"00000000-0000-0000-0000-000000000000",
                                 "createdAt":"2020-01-01T00:00:00Z",
                                 "title":"Ship it",
                                 "priority":"MEDIUM",
                                 "status":"DONE",
                                 "position":99,
                                 "aiGenerated":true,
                                 "generationLocale":"zz"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.aiGenerated").value(false))
                // status and position are server-owned (AD-14): client's DONE / 99 are dropped;
                // response carries the server-assigned defaults TODO / 0.
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.position").value(0));
    }

    // ------------------------------------------------------------------- PATCH /api/cards/{id}

    @Test
    void patchInlineEditReturnsOnePartition() throws Exception {
        UUID id = UUID.randomUUID();
        BoardPartitionsResponse response = new BoardPartitionsResponse(
                List.of(sampleDto(id, "Renamed", CardStatus.TODO, 0)),
                null,
                null);
        when(cardService.patch(eq(id), any(PatchCardRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/cards/" + id)
                        .contentType("application/json")
                        .content("""
                                {"title":"Renamed"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todo").isArray())
                .andExpect(jsonPath("$.todo[0].title").value("Renamed"))
                .andExpect(jsonPath("$.inProgress").doesNotExist())
                .andExpect(jsonPath("$.done").doesNotExist());
    }

    @Test
    void patchCrossColumnMoveReturnsTwoPartitions() throws Exception {
        UUID id = UUID.randomUUID();
        BoardPartitionsResponse response = new BoardPartitionsResponse(
                List.of(sampleDto(UUID.randomUUID(), "A", CardStatus.TODO, 0)),
                List.of(sampleDto(id, "C", CardStatus.IN_PROGRESS, 0)),
                null);
        when(cardService.patch(eq(id), any(PatchCardRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/cards/" + id)
                        .contentType("application/json")
                        .content("""
                                {"status":"IN_PROGRESS","position":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todo").isArray())
                .andExpect(jsonPath("$.inProgress").isArray())
                .andExpect(jsonPath("$.done").doesNotExist());
    }

    @Test
    void patchNullTitleReturns400ValidationFailedEnvelope() throws Exception {
        // Matrix row 8 — round-trip verification that `{"title": null}` surfaces as a
        // VALIDATION_FAILED envelope. The service's internal required-not-null guard is already
        // unit-tested; here we stub it to throw and assert the handler path lands at 400.
        UUID id = UUID.randomUUID();
        when(cardService.patch(eq(id), any(PatchCardRequest.class)))
                .thenThrow(new fr.marwen.kanban.exception.ValidationException(
                        "title: must not be null"));

        mockMvc.perform(patch("/api/cards/" + id)
                        .contentType("application/json")
                        .content("""
                                {"title":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("title")));
    }

    @Test
    void patchUnknownIdReturns404WithEnvelope() throws Exception {
        UUID id = UUID.randomUUID();
        when(cardService.patch(eq(id), any(PatchCardRequest.class)))
                .thenThrow(new EntityNotFoundException("Card " + id + " not found"));

        mockMvc.perform(patch("/api/cards/" + id)
                        .contentType("application/json")
                        .content("""
                                {"title":"X"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ------------------------------------------------------------------- DELETE /api/cards/{id}

    @Test
    void deleteReturns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(cardService).delete(id);

        mockMvc.perform(delete("/api/cards/" + id))
                .andExpect(status().isNoContent());

        verify(cardService).delete(id);
    }

    // ------------------------------------------------------------------- POST /api/cards/batch

    @Test
    void batchHappyPathReturns201WithArray() throws Exception {
        when(cardService.batch(any(CreateBatchRequest.class)))
                .thenReturn(List.of(
                        sampleDto(UUID.randomUUID(), "One", CardStatus.TODO, 0),
                        sampleDto(UUID.randomUUID(), "Two", CardStatus.TODO, 1)));

        mockMvc.perform(post("/api/cards/batch")
                        .contentType("application/json")
                        .content("""
                                {"locale":"fr","cards":[
                                  {"title":"One","priority":"MEDIUM"},
                                  {"title":"Two","priority":"HIGH"}
                                ]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("One"))
                .andExpect(jsonPath("$[1].title").value("Two"));
    }

    @Test
    void batchWithEmptyCardsListReturns400() throws Exception {
        mockMvc.perform(post("/api/cards/batch")
                        .contentType("application/json")
                        .content("""
                                {"locale":"fr","cards":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cards")));
    }

    @Test
    void batchWithInvalidLocaleReturns400() throws Exception {
        mockMvc.perform(post("/api/cards/batch")
                        .contentType("application/json")
                        .content("""
                                {"locale":"zz","cards":[{"title":"One","priority":"MEDIUM"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void batchWithOneInvalidCardIsAtomicallyRejected() throws Exception {
        // Matrix row 12 — AD-8 all-or-nothing. 4 valid + 1 blank-title card in the same list:
        // the whole batch fails with 400, service is NEVER called (no partial persist).
        mockMvc.perform(post("/api/cards/batch")
                        .contentType("application/json")
                        .content("""
                                {"locale":"fr","cards":[
                                  {"title":"One","priority":"MEDIUM"},
                                  {"title":"Two","priority":"HIGH"},
                                  {"title":"Three","priority":"LOW"},
                                  {"title":"Four","priority":"MEDIUM"},
                                  {"title":"","priority":"MEDIUM"}
                                ]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                // Spring's field-path identifies the failing card: `cards[4].title`.
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("cards[4]")));

        verify(cardService, never()).batch(any(CreateBatchRequest.class));
    }

    // ------------------------------------------------------------------- CORS preflight

    @Test
    void corsPreflightFromAllowedOriginReturns200WithHeaders() throws Exception {
        // Matrix row 18 — AD-12: dev-mode CORS allow-list permits http://localhost:4200.
        // Also asserts Access-Control-Allow-Headers carries both Content-Type (manual add) AND
        // Accept-Language (needed for FR/EN error-envelope locale switching). Dropping either
        // would silently break the frontend.
        var response = mockMvc.perform(options("/api/cards")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,Accept-Language"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andReturn()
                .getResponse();
        assertThat(response.getHeader("Access-Control-Allow-Methods"))
                .as("every HTTP verb the API exposes is allowed by the preflight response")
                .isNotNull()
                .contains("GET", "POST", "PATCH", "DELETE", "OPTIONS");
        assertThat(response.getHeader("Access-Control-Allow-Headers"))
                .as("both Content-Type and Accept-Language must be preflight-allowed")
                .isNotNull()
                .contains("Content-Type", "Accept-Language");
    }

    @Test
    void corsPreflightFromDisallowedOriginReturns403() throws Exception {
        // Matrix row 19 — AD-12: any origin outside the allow-list fails the preflight.
        mockMvc.perform(options("/api/cards")
                        .header("Origin", "http://evil.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------- helpers

    private static CardDto sampleDto(UUID id) {
        return sampleDto(id, "Ship it", CardStatus.TODO, 0);
    }

    private static CardDto sampleDto(UUID id, String title, CardStatus status, int position) {
        return new CardDto(id, title, null, Priority.MEDIUM, status, position, false, null, Instant.now());
    }
}
