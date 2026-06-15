package com.pfe.docextraction.repository;

import com.pfe.docextraction.entity.User;
import com.pfe.docextraction.enums.AccountStatus;
import com.pfe.docextraction.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findByIsActiveTrueAndDeletedAtIsNull();

    List<User> findByRoleAndDeletedAtIsNull(UserRole role);

    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = u.failedAttempts + 1 WHERE u.id = :userId")
    void incrementFailedAttempts(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = 0, u.lastLogin = CURRENT_TIMESTAMP WHERE u.id = :userId")
    void resetFailedAttemptsAndUpdateLogin(@Param("userId") UUID userId);

   
    long countByAccountStatus(AccountStatus accountStatus);
}