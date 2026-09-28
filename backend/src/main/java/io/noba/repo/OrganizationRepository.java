package io.noba.repo;

import io.noba.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

	boolean existsBySlug(String slug);
}
