package io.noba.repo;

import io.noba.domain.Counter;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CounterRepository extends JpaRepository<Counter, Long> {

	List<Counter> findByBranchIdOrderByName(Long branchId);

	Optional<Counter> findByCurrentAgentId(Long agentId);

	/** Guichets ouverts qui traitent ce service (sert à l'estimation du temps d'attente). */
	@Query("select count(c) from Counter c join c.services s "
			+ "where s.id = :serviceId and c.active = true and c.currentAgent is not null")
	long countOpenForService(Long serviceId);
}
