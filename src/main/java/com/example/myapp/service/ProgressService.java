package com.example.myapp.service;

import com.example.myapp.model.ListeningProgress;
import com.example.myapp.model.Tale;
import com.example.myapp.model.User;
import com.example.myapp.repository.ListeningProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ProgressService {

    private final ListeningProgressRepository progressRepository;

    public ProgressService(ListeningProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
    }

    public Optional<ListeningProgress> find(User user, Tale tale) {
        return progressRepository.findByUserAndTale(user, tale);
    }

    public List<ListeningProgress> findByUser(User user) {
        return progressRepository.findByUserOrderByUpdatedAtDesc(user);
    }

    @Transactional
    public void record(User user, Tale tale, int seconds, Integer duration, boolean finished) {
        ListeningProgress progress = progressRepository.findByUserAndTale(user, tale)
                .orElseGet(() -> new ListeningProgress(user, tale));
        progress.setSeconds(Math.max(0, seconds));
        if (duration != null && duration > 0) {
            progress.setDuration(duration);
        }
        // listened time only ever grows (furthest point reached), so replays don't re-count
        int reached = Math.max(0, seconds);
        if (progress.getDuration() != null && progress.getDuration() > 0) {
            reached = Math.min(reached, progress.getDuration());
        }
        progress.setListenedSeconds(Math.max(progress.getListenedSeconds(), reached));
        if (finished) {
            progress.setFinished(true);
            if (progress.getDuration() != null && progress.getDuration() > 0) {
                progress.setListenedSeconds(progress.getDuration());
            }
        }
        progress.setUpdatedAt(Instant.now());
        progressRepository.save(progress);
    }

    /** Total unique listening time (seconds) across all tales, replays excluded. */
    public long totalListenedSeconds(User user) {
        return progressRepository.findByUserOrderByUpdatedAtDesc(user).stream()
                .mapToLong(ListeningProgress::getListenedSeconds)
                .sum();
    }
}
