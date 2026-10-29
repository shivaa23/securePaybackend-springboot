package com.securepay.modules.service.repository;

import com.securepay.modules.service.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository for {@link ServiceEntity} (service catalogue). */
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    Optional<ServiceEntity> findByName(String name);

    Optional<ServiceEntity> findByRoute(String route);
    boolean existsByCode(String code);

    java.util.List<com.securepay.modules.service.entity.ServiceEntity> findBySubType(String subType);
}
