package io.noba.repo;

import io.noba.domain.StaffUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {

	Optional<StaffUser> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	List<StaffUser> findByOrganizationIdOrderByFullName(Long organizationId);

	long countByOrganizationId(Long organizationId);

	@Modifying
	@Query("update StaffUser u set u.email = :newEmail where lower(u.email) = lower(:oldEmail)")
	int renameEmail(String oldEmail, String newEmail);
}
