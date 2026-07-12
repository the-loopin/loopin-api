package com.loopin.api.users.repository;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.users.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

    boolean existsByGoogleId(String googleId);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    Optional<User> findByPublicIdAndDeletedAtIsNull(UUID publicId);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByEmailAndIsActiveTrueAndDeletedAtIsNull(String email);

    Optional<User> findByGoogleIdAndDeletedAtIsNull(String googleId);

    List<User> findAllByDeletedAtIsNull();

    List<User> findAllByIsActiveTrueAndDeletedAtIsNull();

    long countByIsActiveTrue();

    long countByRoleAndIsActiveTrue(Role role);

    Page<User> findAllByIsActiveTrue(Pageable pageable);
}
