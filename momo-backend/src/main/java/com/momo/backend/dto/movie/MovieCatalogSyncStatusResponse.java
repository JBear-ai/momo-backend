package com.momo.backend.dto.movie;

import java.time.LocalDateTime;

public record MovieCatalogSyncStatusResponse(
        String phase,
        int nextPage,
        int totalPages,
        long totalCount,
        Integer startYear,
        Integer endYear,
        LocalDateTime lastCompletedAt,
        String lastError
) {
}
