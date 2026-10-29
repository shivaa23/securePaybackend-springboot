package com.securepay.modules.admin.repository;

import com.securepay.modules.admin.entity.RouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repository for {@link RouteEntity}. */
public interface RouteRepository extends JpaRepository<RouteEntity, Long> {

    /** Active routes (status = 1), ordered as inserted. */
    List<RouteEntity> findByStatusOrderByIdAsc(Integer status);
}
