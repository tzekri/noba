package io.noba.repo;

import io.noba.domain.StaffUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {

	Optional<StaffUser> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	List<StaffUser> findByOrganizationIdOrderByFullName(Long organizationId);

	long countByOrganizationId(Long organizationId);
}
