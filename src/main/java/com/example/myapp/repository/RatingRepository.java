package com.example.myapp.repository;

import com.example.myapp.model.Rating;
import com.example.myapp.model.Tale;
import com.example.myapp.model.TaleStatus;
import com.example.myapp.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByTaleAndUser(Tale tale, User user);

    long countByTale(Tale tale);

    @Query("SELECT COALESCE(AVG(r.stars), 0) FROM Rating r WHERE r.tale = :tale")
    double averageByTale(@Param("tale") Tale tale);

    /** Highest-rated approved tales by ratings activity since {@code from} (monthly top list). */
    @Query("""
            SELECT r.tale FROM Rating r
            WHERE r.updatedAt >= :from AND r.tale.status = :status
            GROUP BY r.tale
            ORDER BY AVG(r.stars) DESC, COUNT(r) DESC
            """)
    List<Tale> topRatedSince(@Param("from") Instant from,
                             @Param("status") TaleStatus status,
                             Pageable pageable);
}
