package com.example.myapp.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tale_id")
    private Tale tale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    /** Replies attach to a top-level comment (one level of nesting). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Comment> replies = new ArrayList<>();

    @Column(nullable = false, length = 1000)
    private String content;

    /** False until the tale's storyteller opens their comments inbox. */
    @Column(nullable = false)
    private boolean seenByStoryteller = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Comment() {
    }

    public Comment(Tale tale, User author, Comment parent, String content) {
        this.tale = tale;
        this.author = author;
        this.parent = parent;
        this.content = content;
        // Own comments on one's tales don't need a notification
        if (author != null && tale != null && tale.getStoryteller() != null
                && author.getId() != null
                && author.getId().equals(tale.getStoryteller().getId())) {
            this.seenByStoryteller = true;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tale getTale() {
        return tale;
    }

    public void setTale(Tale tale) {
        this.tale = tale;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Comment getParent() {
        return parent;
    }

    public void setParent(Comment parent) {
        this.parent = parent;
    }

    public List<Comment> getReplies() {
        return replies;
    }

    public void setReplies(List<Comment> replies) {
        this.replies = replies;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isSeenByStoryteller() {
        return seenByStoryteller;
    }

    public void setSeenByStoryteller(boolean seenByStoryteller) {
        this.seenByStoryteller = seenByStoryteller;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
