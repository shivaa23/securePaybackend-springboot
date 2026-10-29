package com.securepay.modules.complains.repository;

import com.securepay.modules.complains.entity.Complains;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link Complains}. */
public interface ComplainsRepository extends JpaRepository<Complains, Long> {
}
