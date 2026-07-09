package com.example.myapp.repository;

import com.example.myapp.model.Favorite;
import com.example.myapp.model.Tale;
import com.example.myapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserAndTale(User user, Tale tale);

    boolean existsByUserAndTale(User user, Tale tale);

    List<Favorite> findByUserOrderByCreatedAtDesc(User user);
}
