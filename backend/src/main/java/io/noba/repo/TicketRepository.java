package io.noba.repo;

import io.noba.domain.Ticket;
import io.noba.domain.TicketStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	Optional<Ticket> findByPublicToken(String publicToken);

	Optional<Ticket> findByBranchIdAndDayAndCode(Long branchId, LocalDate day, String code);

	/** Personnes devant ce ticket dans la même file. */
	@Query("select count(t) from Ticket t where t.service.id = :serviceId and t.day = :day "
			+ "and t.status = io.noba.domain.TicketStatus.WAITING and t.queuedAt < :queuedAt")
	long countAhead(Long serviceId, LocalDate day, Instant queuedAt);

	long countByServiceIdAndDayAndStatus(Long serviceId, LocalDate day, TicketStatus status);

	long countByServiceIdAndDay(Long serviceId, LocalDate day);

	/** Prochain ticket à appeler pour un ensemble de services, verrouillé pour qu'un seul guichet l'obtienne. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Ticket t where t.service.id in :serviceIds and t.day = :day "
			+ "and t.status = io.noba.domain.TicketStatus.WAITING order by t.queuedAt asc")
	List<Ticket> lockNextWaiting(Collection<Long> serviceIds, LocalDate day, Pageable pageable);

	@Query("select t from Ticket t join fetch t.service where t.branch.id = :branchId and t.day = :day "
			+ "and t.status = io.noba.domain.TicketStatus.WAITING order by t.queuedAt asc")
	List<Ticket> findWaiting(Long branchId, LocalDate day, Pageable pageable);

	@Query("select t from Ticket t join fetch t.service left join fetch t.counter "
			+ "where t.branch.id = :branchId and t.day = :day and t.calledAt is not null "
			+ "and t.status in :statuses order by t.calledAt desc")
	List<Ticket> findRecentlyCalled(Long branchId, LocalDate day, Collection<TicketStatus> statuses, Pageable pageable);

	@Query("select t from Ticket t where t.counter.id = :counterId and t.status in "
			+ "(io.noba.domain.TicketStatus.CALLED, io.noba.domain.TicketStatus.SERVING)")
	List<Ticket> findCurrentAtCounter(Long counterId);

	/** Derniers tickets terminés d'un service, pour la moyenne glissante de durée de traitement. */
	@Query("select t from Ticket t where t.service.id = :serviceId and t.day = :day "
			+ "and t.status = io.noba.domain.TicketStatus.DONE and t.calledAt is not null "
			+ "and t.completedAt is not null order by t.completedAt desc")
	List<Ticket> findRecentDone(Long serviceId, LocalDate day, Pageable pageable);

	@Query("select t from Ticket t join fetch t.service left join fetch t.agent "
			+ "where t.branch.id = :branchId and t.day between :from and :to")
	List<Ticket> findForStats(Long branchId, LocalDate from, LocalDate to);

	@Modifying
	@Query("update Ticket t set t.status = io.noba.domain.TicketStatus.EXPIRED where t.day < :today and t.status in "
			+ "(io.noba.domain.TicketStatus.WAITING, io.noba.domain.TicketStatus.CALLED, io.noba.domain.TicketStatus.SERVING)")
	int expireBefore(LocalDate today);
}
