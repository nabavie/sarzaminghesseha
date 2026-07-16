package com.example.myapp.repository;

import com.example.myapp.model.Comment;
import com.example.myapp.model.Tale;
import com.example.myapp.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTaleAndParentIsNullOrderByCreatedAtAsc(Tale tale);

    Page<Comment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByTale(Tale tale);

    @Query("""
            SELECT DISTINCT c FROM Comment c
            JOIN FETCH c.tale t
            JOIN FETCH c.author
            WHERE t.storyteller = :storyteller
              AND c.author <> :storyteller
            ORDER BY c.seenByStoryteller ASC, c.createdAt DESC
            """)
    List<Comment> findForStoryteller(@Param("storyteller") User storyteller);

    @Query("""
            SELECT COUNT(c) FROM Comment c
            WHERE c.tale.storyteller = :storyteller
              AND c.author <> :storyteller
              AND c.seenByStoryteller = false
            """)
    long countUnseenForStoryteller(@Param("storyteller") User storyteller);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Comment c
            SET c.seenByStoryteller = true
            WHERE c.tale.storyteller = :storyteller
              AND c.seenByStoryteller = false
            """)
    int markAllSeenForStoryteller(@Param("storyteller") User storyteller);
}
