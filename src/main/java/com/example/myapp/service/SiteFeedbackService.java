package com.example.myapp.service;

import com.example.myapp.dto.SiteFeedbackForm;
import com.example.myapp.model.SiteFeedback;
import com.example.myapp.repository.SiteFeedbackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteFeedbackService {

    private final SiteFeedbackRepository repository;

    public SiteFeedbackService(SiteFeedbackRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SiteFeedback submit(SiteFeedbackForm form) {
        SiteFeedback feedback = new SiteFeedback(form.getName().trim(), form.getMessage().trim());
        return repository.save(feedback);
    }

    public Page<SiteFeedback> findAll(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public long countUnseen() {
        return repository.countBySeenFalse();
    }

    @Transactional
    public void markSeen(Long id) {
        repository.findById(id).ifPresent(feedback -> {
            feedback.setSeen(true);
            repository.save(feedback);
        });
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
