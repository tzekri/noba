package io.noba.repo;

import io.noba.domain.QueueService;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueServiceRepository extends JpaRepository<QueueService, Long> {

	List<QueueService> findByBranchIdOrderBySortOrderAscNameAsc(Long branchId);

	List<QueueService> findByBranchIdAndActiveTrueOrderBySortOrderAscNameAsc(Long branchId);
}
