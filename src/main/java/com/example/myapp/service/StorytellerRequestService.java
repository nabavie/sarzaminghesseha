package com.example.myapp.service;

import com.example.myapp.model.RequestStatus;
import com.example.myapp.model.Role;
import com.example.myapp.model.StorytellerRequest;
import com.example.myapp.model.User;
import com.example.myapp.repository.StorytellerRequestRepository;
import com.example.myapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class StorytellerRequestService {

    private final StorytellerRequestRepository requestRepository;
    private final UserRepository userRepository;

    public StorytellerRequestService(StorytellerRequestRepository requestRepository,
                                     UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    public Optional<StorytellerRequest> latestForUser(User user) {
        return requestRepository.findFirstByUserOrderByCreatedAtDesc(user);
    }

    public boolean hasPending(User user) {
        return requestRepository.existsByUserAndStatus(user, RequestStatus.PENDING);
    }

    public List<StorytellerRequest> pending() {
        return requestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING);
    }

    public List<StorytellerRequest> all() {
        return requestRepository.findAllByOrderByCreatedAtDesc();
    }

    public long pendingCount() {
        return requestRepository.countByStatus(RequestStatus.PENDING);
    }

    public Optional<StorytellerRequest> findById(Long id) {
        return requestRepository.findById(id);
    }

    /**
     * Requests from the dashboard are granted immediately (every tale still goes
     * through admin review before publication, so this is safe). The request is
     * stored as an auto-approved record for the admin history page.
     */
    @Transactional
    public boolean submit(User user, String message) {
        if (user.hasRole(Role.STORYTELLER)) {
            return false;
        }
        StorytellerRequest request = new StorytellerRequest(user,
                message == null || message.isBlank() ? null : message.trim());
        request.setStatus(RequestStatus.APPROVED);
        request.setAdminNote("تأیید خودکار");
        request.setDecidedAt(Instant.now());
        requestRepository.save(request);

        user.getRoles().add(Role.STORYTELLER);
        userRepository.save(user);
        return true;
    }

    @Transactional
    public void approve(StorytellerRequest request, String note) {
        request.setStatus(RequestStatus.APPROVED);
        request.setAdminNote(note == null || note.isBlank() ? null : note.trim());
        request.setDecidedAt(Instant.now());
        requestRepository.save(request);

        User user = request.getUser();
        user.getRoles().add(Role.STORYTELLER);
        userRepository.save(user);
    }

    @Transactional
    public void reject(StorytellerRequest request, String note) {
        request.setStatus(RequestStatus.REJECTED);
        request.setAdminNote(note == null || note.isBlank() ? null : note.trim());
        request.setDecidedAt(Instant.now());
        requestRepository.save(request);
    }
}
