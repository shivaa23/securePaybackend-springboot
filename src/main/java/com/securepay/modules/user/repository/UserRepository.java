package com.securepay.modules.user.repository;

import com.securepay.modules.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link User}. Includes the pessimistic-lock lookup used by the
 * wallet ledger services (port of the legacy {@code lockForUpdate()}, doc 06 §6.5).
 *
 * <p>Extends {@link JpaSpecificationExecutor} so {@code getUsers} can apply the legacy dynamic
 * filters (mobile/id/role/parent/establishment) with pagination.</p>
 */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByApiToken(String apiToken);

    Optional<User> findByMobile(String mobile);

    Optional<User> findByEmail(String email);

    /** Direct children of a user (parent = userId). */
    List<User> findByParent(Long parent);

    /** Pessimistic write lock on the user row — SELECT ... FOR UPDATE, the legacy lock-for-update (doc 10 §10.3). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
    boolean existsByPlanId(Long planId);
    java.util.Optional<User> findFirstByMobile(String mobile);
}
