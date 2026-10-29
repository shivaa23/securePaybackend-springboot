package com.securepay.modules.plan.repository;

import com.securepay.modules.plan.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link Plan}. */
public interface PlanRepository extends JpaRepository<Plan, Long> {
}
