package com.example.myapp.service;

import com.example.myapp.model.Rating;
import com.example.myapp.model.Tale;
import com.example.myapp.model.TaleStatus;
import com.example.myapp.model.User;
import com.example.myapp.repository.RatingRepository;
import com.example.myapp.util.PersianDateUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final PersianDateUtil persianDate;

    public RatingService(RatingRepository ratingRepository, PersianDateUtil persianDate) {
        this.ratingRepository = ratingRepository;
        this.persianDate = persianDate;
    }

    @Transactional
    public void rate(User user, Tale tale, int stars) {
        int clamped = Math.max(1, Math.min(5, stars));
        Rating rating = ratingRepository.findByTaleAndUser(tale, user)
                .orElseGet(() -> new Rating(tale, user, clamped));
        rating.setStars(clamped);
        rating.setUpdatedAt(Instant.now());
        ratingRepository.save(rating);
    }

    public double average(Tale tale) {
        return ratingRepository.averageByTale(tale);
    }

    public long count(Tale tale) {
        return ratingRepository.countByTale(tale);
    }

    public Integer userStars(User user, Tale tale) {
        return ratingRepository.findByTaleAndUser(tale, user)
                .map(Rating::getStars)
                .orElse(null);
    }

    /** Top rated tales of the current Jalali (Persian) month, by rating activity. */
    public List<Tale> topOfMonth(int limit) {
        Instant monthStart = persianDate.startOfCurrentJalaliMonth();
        return ratingRepository.topRatedSince(monthStart, TaleStatus.APPROVED, PageRequest.of(0, limit));
    }
}
