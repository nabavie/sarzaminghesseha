package com.example.myapp.repository;

import com.example.myapp.model.Role;
import com.example.myapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findFirstByMobileAndEnabledTrue(String mobile);

    boolean existsByMobileAndEnabledTrue(String mobile);

    boolean existsByMobileAndEnabledTrueAndIdNot(String mobile, Long id);

    List<User> findByEnabledTrueAndMobileIsNotNull();

    long countByEnabledTrueAndMobileIsNotNull();

    @Query("""
            SELECT u FROM User u
            WHERE u.enabled = true
              AND u.mobile IS NOT NULL
              AND (:q = '' OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR u.mobile LIKE CONCAT('%', :q, '%'))
            ORDER BY u.displayName ASC
            """)
    List<User> searchEnabledWithMobile(@Param("q") String q, org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN u.roles r
            WHERE r = :role
              AND (:q = '' OR u.displayName LIKE CONCAT('%', :q, '%') OR u.username LIKE CONCAT('%', :q, '%'))
            ORDER BY u.displayName ASC
            """)
    List<User> searchStorytellers(@Param("role") Role role, @Param("q") String q);
}
