package com.momo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "movie_catalog_sync_state")
public class MovieCatalogSyncState {

    @Id
    private Integer id;

    @Column(nullable = false, length = 20)
    private String phase;

    @Column(nullable = false)
    private int nextPage;

    @Column(nullable = false)
    private int totalPages;

    @Column(nullable = false)
    private long totalCount;

    private Integer startYear;
    private Integer endYear;
    private LocalDateTime lastCompletedAt;
    private LocalDateTime retryAfter;

    @Column(length = 1000)
    private String lastError;

    private LocalDateTime updatedAt;

    public static MovieCatalogSyncState initial() {
        MovieCatalogSyncState state = new MovieCatalogSyncState();
        state.id = 1;
        state.phase = "INITIAL";
        state.nextPage = 1;
        state.totalPages = 0;
        state.totalCount = 0;
        state.updatedAt = LocalDateTime.now();
        return state;
    }

    public void completePage(int page, int pageSize, long count, LocalDateTime now) {
        this.totalCount = count;
        this.totalPages = (int) Math.ceil((double) count / pageSize);
        this.nextPage = page + 1;
        this.retryAfter = null;
        this.lastError = null;
        this.updatedAt = now;

        if (count == 0 || page >= this.totalPages) {
            this.phase = "IDLE";
            this.nextPage = 1;
            this.lastCompletedAt = now;
            this.startYear = null;
            this.endYear = null;
        }
    }

    public void startRecentSync(int startYear, int endYear, LocalDateTime now) {
        this.phase = "RECENT";
        this.nextPage = 1;
        this.totalPages = 0;
        this.totalCount = 0;
        this.startYear = startYear;
        this.endYear = endYear;
        this.retryAfter = null;
        this.lastError = null;
        this.updatedAt = now;
    }

    public void recordFailure(String message, LocalDateTime now) {
        this.lastError = message == null ? "알 수 없는 수집 오류" : message.substring(0, Math.min(message.length(), 1000));
        this.retryAfter = now.plusMinutes(1);
        this.updatedAt = now;
    }

    public boolean canRetry(LocalDateTime now) {
        return retryAfter == null || !retryAfter.isAfter(now);
    }

    public boolean needsRecentSync(LocalDateTime now) {
        return "IDLE".equals(phase)
                && (lastCompletedAt == null || lastCompletedAt.toLocalDate().isBefore(now.toLocalDate()));
    }

    public boolean isActive() {
        return "INITIAL".equals(phase) || "RECENT".equals(phase);
    }
}
