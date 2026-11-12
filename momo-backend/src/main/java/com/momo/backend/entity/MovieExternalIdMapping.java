package com.momo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "movie_external_id_mappings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_movie_external_provider_id",
                        columnNames = {"provider", "external_id"}
                )
        },
        indexes = {
                @Index(name = "ix_movie_external_provider_code", columnList = "provider, movie_code")
        }
)
public class MovieExternalIdMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mappingId;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(name = "movie_code", nullable = false, length = 20)
    private String movieCode;

    public MovieExternalIdMapping(String provider, String externalId, String movieCode) {
        this.provider = provider;
        this.externalId = externalId;
        this.movieCode = movieCode;
    }
}
