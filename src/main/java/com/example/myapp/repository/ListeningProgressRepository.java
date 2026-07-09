package com.example.myapp.repository;

import com.example.myapp.model.ListeningProgress;
import com.example.myapp.model.Tale;
import com.example.myapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListeningProgressRepository extends JpaRepository<ListeningProgress, Long> {

    Optional<ListeningProgress> findByUserAndTale(User user, Tale tale);

    List<ListeningProgress> findByUserOrderByUpdatedAtDesc(User user);
}
