package com.securepay.modules.signup.repository;

import com.securepay.modules.signup.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link District}. */
public interface DistrictRepository extends JpaRepository<District, Long> {
}
