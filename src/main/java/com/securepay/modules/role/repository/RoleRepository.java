package com.securepay.modules.role.repository;

import com.securepay.modules.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link Role}. */
public interface RoleRepository extends JpaRepository<Role, Long> {
}
