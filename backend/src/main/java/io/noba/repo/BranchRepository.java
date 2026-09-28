package io.noba.repo;

import io.noba.domain.Branch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {

	Optional<Branch> findByCode(String code);

	boolean existsByCode(String code);

	List<Branch> findByOrganizationIdOrderByName(Long organizationId);

	long countByOrganizationId(Long organizationId);
}
