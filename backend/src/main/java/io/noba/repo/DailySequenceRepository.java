package io.noba.repo;

import io.noba.domain.DailySequence;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface DailySequenceRepository extends JpaRepository<DailySequence, Long> {

	boolean existsByServiceIdAndDay(Long serviceId, LocalDate day);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from DailySequence s where s.serviceId = :serviceId and s.day = :day")
	Optional<DailySequence> lock(Long serviceId, LocalDate day);
}
