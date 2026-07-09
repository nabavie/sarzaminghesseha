package com.example.myapp.repository;

import com.example.myapp.model.Tale;
import com.example.myapp.model.TaleStatus;
import com.example.myapp.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaleRepository extends JpaRepository<Tale, Long> {

    Page<Tale> findByStatusOrderByCreatedAtDesc(TaleStatus status, Pageable pageable);

    List<Tale> findByCategoriesContaining(com.example.myapp.model.Category category);

    List<Tale> findByStorytellerOrderByCreatedAtDesc(User storyteller);

    long countByStatus(TaleStatus status);

    @Query("""
            SELECT t FROM Tale t
            WHERE t.status = :status
              AND (:q = '' OR t.title LIKE CONCAT('%', :q, '%') OR t.description LIKE CONCAT('%', :q, '%'))
              AND (:categoryId IS NULL OR :categoryId IN (SELECT c.id FROM t.categories c))
            ORDER BY t.createdAt DESC
            """)
    Page<Tale> search(@Param("status") TaleStatus status,
                      @Param("q") String q,
                      @Param("categoryId") Long categoryId,
                      Pageable pageable);
}
