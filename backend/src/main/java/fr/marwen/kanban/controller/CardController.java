package fr.marwen.kanban.controller;

import fr.marwen.kanban.dto.BoardPartitionsResponse;
import fr.marwen.kanban.dto.CardDto;
import fr.marwen.kanban.dto.CreateBatchRequest;
import fr.marwen.kanban.dto.CreateCardRequest;
import fr.marwen.kanban.dto.PatchCardRequest;
import fr.marwen.kanban.service.CardService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP surface for {@code /api/cards} (architecture AD-1, AD-13).
 *
 * <p>Thin routing shell: validate the payload, delegate to {@link CardService}, shape the HTTP
 * response. No business logic, no transaction handling (AD-7), no repository calls (AD-1). Error
 * translation is centralized in {@code GlobalExceptionHandler} so the controller never catches —
 * exceptions propagate.
 */
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    public List<CardDto> list() {
        return cardService.findAll();
    }

    @PostMapping
    public ResponseEntity<CardDto> create(@Valid @RequestBody CreateCardRequest req) {
        CardDto created = cardService.create(req);
        return ResponseEntity.created(URI.create("/api/cards/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    public BoardPartitionsResponse patch(
            @PathVariable UUID id,
            @RequestBody PatchCardRequest req) {
        // Bean validation is a no-op for `JsonNullable<T>` fields — CardService runs the
        // required-field null guard (AD-13) and surfaces 400 via ValidationException.
        return cardService.patch(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        cardService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch")
    public ResponseEntity<List<CardDto>> batch(@Valid @RequestBody CreateBatchRequest req) {
        List<CardDto> created = cardService.batch(req);
        return ResponseEntity.status(201).body(created);
    }
}
