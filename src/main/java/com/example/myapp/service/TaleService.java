package com.example.myapp.service;

import com.example.myapp.model.Tale;
import com.example.myapp.model.TaleStatus;
import com.example.myapp.model.User;
import com.example.myapp.repository.TaleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class TaleService {

    private final TaleRepository taleRepository;

    public TaleService(TaleRepository taleRepository) {
        this.taleRepository = taleRepository;
    }

    public Optional<Tale> findById(Long id) {
        return taleRepository.findById(id);
    }

    public List<Tale> findByStoryteller(User storyteller) {
        return taleRepository.findByStorytellerOrderByCreatedAtDesc(storyteller);
    }

    public Page<Tale> searchApproved(String q, Long categoryId, int page, int size) {
        return searchApproved(q, categoryId, null, page, size);
    }

    public Page<Tale> searchApproved(String q, Long categoryId, Long storytellerId, int page, int size) {
        String query = q == null ? "" : q.trim();
        return taleRepository.search(TaleStatus.APPROVED, query, categoryId, storytellerId,
                PageRequest.of(page, size));
    }

    public Page<Tale> findApprovedByStoryteller(User storyteller, int page, int size) {
        return taleRepository.findByStorytellerAndStatusOrderByCreatedAtDesc(
                storyteller, TaleStatus.APPROVED, PageRequest.of(page, size));
    }

    public long countApprovedByStoryteller(User storyteller) {
        return taleRepository.countByStorytellerAndStatus(storyteller, TaleStatus.APPROVED);
    }

    public List<Tale> findRecentApproved(int limit) {
        return taleRepository.findRecentApproved(TaleStatus.APPROVED, PageRequest.of(0, limit));
    }

    public Page<Tale> findByStatus(TaleStatus status, Pageable pageable) {
        return taleRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    public long countByStatus(TaleStatus status) {
        return taleRepository.countByStatus(status);
    }

    @Transactional
    public Tale save(Tale tale) {
        return taleRepository.save(tale);
    }

    @Transactional
    public void approve(Tale tale, String note) {
        tale.setStatus(TaleStatus.APPROVED);
        tale.setReviewNote(note == null || note.isBlank() ? null : note.trim());
        tale.setApprovedAt(Instant.now());
        taleRepository.save(tale);
    }

    @Transactional
    public void reject(Tale tale, String note) {
        tale.setStatus(TaleStatus.REJECTED);
        tale.setReviewNote(note == null || note.isBlank() ? null : note.trim());
        tale.setApprovedAt(null);
        taleRepository.save(tale);
    }
}
