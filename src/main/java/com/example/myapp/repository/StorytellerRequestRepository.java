package com.example.myapp.repository;

import com.example.myapp.model.RequestStatus;
import com.example.myapp.model.StorytellerRequest;
import com.example.myapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorytellerRequestRepository extends JpaRepository<StorytellerRequest, Long> {

    Optional<StorytellerRequest> findFirstByUserOrderByCreatedAtDesc(User user);

    boolean existsByUserAndStatus(User user, RequestStatus status);

    List<StorytellerRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status);

    List<StorytellerRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(RequestStatus status);
}
