package com.example.myapp.service;

import com.example.myapp.model.Favorite;
import com.example.myapp.model.Tale;
import com.example.myapp.model.User;
import com.example.myapp.repository.FavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    public FavoriteService(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    public boolean isFavorite(User user, Tale tale) {
        return favoriteRepository.existsByUserAndTale(user, tale);
    }

    public List<Favorite> findByUser(User user) {
        return favoriteRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /** Adds the tale to the user's list, or removes it if already there. Returns the new state. */
    @Transactional
    public boolean toggle(User user, Tale tale) {
        var existing = favoriteRepository.findByUserAndTale(user, tale);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        }
        favoriteRepository.save(new Favorite(user, tale));
        return true;
    }
}
