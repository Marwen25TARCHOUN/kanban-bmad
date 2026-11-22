package fr.marwen.kanban.repository;

import fr.marwen.kanban.domain.Card;
import fr.marwen.kanban.domain.CardStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for {@link Card}.
 *
 * <p>Architecture AD-1: only services call this; controllers never do. Architecture AD-7:
 * repositories carry no transaction annotation of their own. No {@code @Repository} — Spring
 * Data auto-registers the proxy bean from {@link JpaRepository}, and the annotation would be
 * dead weight.
 */
public interface CardRepository extends JpaRepository<Card, UUID> {

    /**
     * Load every card in {@code status}, ordered by {@code position}, under a
     * {@code SELECT ... FOR UPDATE} pessimistic lock (architecture AD-4).
     *
     * <p>Used by the move/delete/create paths in {@code CardService} to serialize concurrent
     * writes against the {@code UNIQUE(status, card_position)} partition before renumbering. Must
     * be called inside a {@code @Transactional} service method — Spring Data silently no-ops the
     * lock outside a transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Card c WHERE c.status = :status ORDER BY c.position")
    List<Card> findByStatusForUpdate(@Param("status") CardStatus status);
}
