package com.momo.backend.repository;

import com.momo.backend.entity.MovieExternalIdMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieExternalIdMappingRepository
        extends JpaRepository<MovieExternalIdMapping, Long> {

    Optional<MovieExternalIdMapping> findByProviderAndExternalId(
            String provider,
            String externalId
    );

    Optional<MovieExternalIdMapping> findFirstByProviderAndMovieCodeOrderByMappingIdAsc(
            String provider,
            String movieCode
    );
}
