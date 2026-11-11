package com.momo.backend.repository;

import com.momo.backend.entity.MovieCatalogSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieCatalogSyncStateRepository extends JpaRepository<MovieCatalogSyncState, Integer> {
}
