package io.noba.repo;

import io.noba.domain.PushSubscription;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

	List<PushSubscription> findByTicketId(Long ticketId);

	List<PushSubscription> findByTicketIdIn(Collection<Long> ticketIds);

	Optional<PushSubscription> findByTicketIdAndEndpoint(Long ticketId, String endpoint);

	long countByTicketId(Long ticketId);

	@Modifying
	@Query("delete from PushSubscription s where s.ticket.id = :ticketId and s.endpoint = :endpoint")
	int deleteByTicketAndEndpoint(Long ticketId, String endpoint);

	/** Abonnement expiré côté navigateur (réponse 404/410 du service push). */
	@Modifying
	@Query("delete from PushSubscription s where s.endpoint = :endpoint")
	int deleteByEndpoint(String endpoint);

	@Modifying
	@Query("delete from PushSubscription s where s.createdAt < :before")
	int deleteOlderThan(Instant before);
}
