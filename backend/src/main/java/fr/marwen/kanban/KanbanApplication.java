package fr.marwen.kanban;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Local AI-Augmented Kanban backend.
 *
 * <p>Scaffolded per story 1 (backend scaffolding, Card entity, H2 persistence) and architecture
 * AD-1 (classic layered backend). Only the {@code domain} and {@code repository} layers are
 * populated in this story; {@code config}, {@code controller}, {@code service}, {@code mapper},
 * {@code dto}, and {@code exception} are anchored as empty packages and filled by subsequent
 * stories.
 */
@SpringBootApplication
public class KanbanApplication {

    public static void main(String[] args) {
        SpringApplication.run(KanbanApplication.class, args);
    }
}
