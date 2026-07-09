package com.example.myapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "listening_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tale_id"}))
public class ListeningProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tale_id")
    private Tale tale;

    @Column(nullable = false)
    private int seconds;

    /**
     * Furthest position (in seconds) the user has ever reached in this tale.
     * Grows monotonically, so replaying an already-heard part never adds time —
     * this is the "time spent" figure shown on the dashboard.
     */
    @Column(nullable = false)
    private int listenedSeconds;

    private Integer duration;

    @Column(nullable = false)
    private boolean finished;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public ListeningProgress() {
    }

    public ListeningProgress(User user, Tale tale) {
        this.user = user;
        this.tale = tale;
    }

    /** Percent listened (0-100), for dashboard progress bars. */
    public int getPercent() {
        if (finished) {
            return 100;
        }
        if (duration == null || duration <= 0) {
            return 0;
        }
        return Math.min(100, Math.round(listenedSeconds * 100f / duration));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Tale getTale() {
        return tale;
    }

    public void setTale(Tale tale) {
        this.tale = tale;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public int getListenedSeconds() {
        return listenedSeconds;
    }

    public void setListenedSeconds(int listenedSeconds) {
        this.listenedSeconds = listenedSeconds;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
