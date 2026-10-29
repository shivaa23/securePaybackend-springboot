package com.securepay.modules.layout.repository;

import com.securepay.modules.layout.entity.SideNav;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link SideNav}. */
public interface SideNavRepository extends JpaRepository<SideNav, Long> {
}
