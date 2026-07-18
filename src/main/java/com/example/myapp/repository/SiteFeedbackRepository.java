package com.example.myapp.repository;

import com.example.myapp.model.SiteFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteFeedbackRepository extends JpaRepository<SiteFeedback, Long> {

    Page<SiteFeedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countBySeenFalse();
}
