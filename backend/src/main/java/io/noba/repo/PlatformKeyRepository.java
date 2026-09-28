package io.noba.repo;

import io.noba.domain.PlatformKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformKeyRepository extends JpaRepository<PlatformKey, String> {
}
