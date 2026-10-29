package com.securepay.modules.misc.repository;

import com.securepay.modules.misc.entity.Uuid;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link Uuid}. */
public interface UuidRepository extends JpaRepository<Uuid, Long> {

    java.util.Optional<Uuid> findFirstByUuid(String uuid);
}
