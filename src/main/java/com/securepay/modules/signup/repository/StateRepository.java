package com.securepay.modules.signup.repository;

import com.securepay.modules.signup.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link State}. */
public interface StateRepository extends JpaRepository<State, Long> {
}
