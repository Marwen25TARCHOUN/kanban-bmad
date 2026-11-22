package fr.marwen.kanban.repository;

import fr.marwen.kanban.domain.Card;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Card}.
 *
 * <p>Architecture AD-1: only services call this; controllers never do. Architecture AD-7:
 * repositories carry no transaction annotation of their own. No {@code @Repository} — Spring
 * Data auto-registers the proxy bean from {@link JpaRepository}, and the annotation would be
 * dead weight.
 */
public interface CardRepository extends JpaRepository<Card, UUID> {
}
