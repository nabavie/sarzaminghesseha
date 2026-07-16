package com.example.myapp.service;

import com.example.myapp.model.Comment;
import com.example.myapp.model.Tale;
import com.example.myapp.model.User;
import com.example.myapp.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public List<Comment> topLevelForTale(Tale tale) {
        return commentRepository.findByTaleAndParentIsNullOrderByCreatedAtAsc(tale);
    }

    public Optional<Comment> findById(Long id) {
        return commentRepository.findById(id);
    }

    public Page<Comment> findAll(Pageable pageable) {
        return commentRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<Comment> forStoryteller(User storyteller) {
        return commentRepository.findForStoryteller(storyteller);
    }

    public long countUnseenForStoryteller(User storyteller) {
        return commentRepository.countUnseenForStoryteller(storyteller);
    }

    @Transactional
    public Comment add(Tale tale, User author, Comment parent, String content) {
        // replies to replies attach to the top-level comment (one level of nesting)
        Comment topLevelParent = parent == null ? null
                : parent.getParent() == null ? parent : parent.getParent();
        return commentRepository.save(new Comment(tale, author, topLevelParent, content.trim()));
    }

    @Transactional
    public void markAllSeenForStoryteller(User storyteller) {
        commentRepository.markAllSeenForStoryteller(storyteller);
    }

    @Transactional
    public void delete(Long id) {
        commentRepository.deleteById(id);
    }
}
