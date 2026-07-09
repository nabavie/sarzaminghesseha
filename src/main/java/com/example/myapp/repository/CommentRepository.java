package com.example.myapp.repository;

import com.example.myapp.model.Comment;
import com.example.myapp.model.Tale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTaleAndParentIsNullOrderByCreatedAtAsc(Tale tale);

    Page<Comment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByTale(Tale tale);
}
