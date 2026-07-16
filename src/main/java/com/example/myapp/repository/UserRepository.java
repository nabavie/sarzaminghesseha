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

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN u.roles r
            WHERE r = :role
              AND (:q = '' OR u.displayName LIKE CONCAT('%', :q, '%') OR u.username LIKE CONCAT('%', :q, '%'))
            ORDER BY u.displayName ASC
            """)
    List<User> searchStorytellers(@Param("role") Role role, @Param("q") String q);
}
