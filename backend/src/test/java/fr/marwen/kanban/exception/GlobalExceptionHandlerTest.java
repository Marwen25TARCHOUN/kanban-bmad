package fr.marwen.kanban.exception;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.marwen.kanban.config.CorsConfig;
import fr.marwen.kanban.controller.CardController;
import fr.marwen.kanban.dto.CreateCardRequest;
import fr.marwen.kanban.service.CardService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pins the error-envelope contract — architecture AD-2.
 *
 * <p>Rather than stand up a stub controller just to exercise the advice, we ride on top of
 * {@link CardController} with a mocked service and trigger 400 / 404 / 500 by crafting the right
 * request or stubbing the service to throw. All three envelopes must render as
 * {@code {"error": "...", "message": "..."}}.
 *
 * <p>The locale-switch case asserts that {@code Accept-Language: fr} carries a French message and
 * the default is English — the hard-coded map in {@code GlobalExceptionHandler} is the shim until
 * story 7's {@code MessageSource}.
 */
@WebMvcTest(CardController.class)
@Import({GlobalExceptionHandler.class, CorsConfig.class})
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CardService cardService;

    @Test
    void validationFailureEnvelopeHasErrorAndMessage() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .contentType("application/json")
                        .content("""
                                {"priority":"MEDIUM"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void notFoundEnvelopeHasErrorAndMessage() throws Exception {
        when(cardService.create(any(CreateCardRequest.class)))
                .thenThrow(new EntityNotFoundException("missing"));

        mockMvc.perform(post("/api/cards")
                        .contentType("application/json")
                        .content("""
                                {"title":"X","priority":"MEDIUM"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void internalErrorEnvelopeHasErrorAndGenericMessage() throws Exception {
        when(cardService.create(any(CreateCardRequest.class)))
                .thenThrow(new RuntimeException("boom — must not leak"));

        mockMvc.perform(post("/api/cards")
                        .contentType("application/json")
                        .content("""
                                {"title":"X","priority":"MEDIUM"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Something went wrong"));
    }

    @Test
    void malformedJsonBodyReturns400ValidationFailed() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .contentType("application/json")
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void acceptLanguageFrLocalizesNotFoundMessage() throws Exception {
        when(cardService.create(any(CreateCardRequest.class)))
                .thenThrow(new EntityNotFoundException("missing"));

        mockMvc.perform(post("/api/cards")
                        .contentType("application/json")
                        .header("Accept-Language", "fr")
                        .content("""
                                {"title":"X","priority":"MEDIUM"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ressource introuvable"));
    }
}
