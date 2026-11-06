package com.momo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// KOBIS에서 조회한 영화 기본정보를 DB에 저장
@Entity
@Getter
@NoArgsConstructor
@Table(name = "movies")
public class Movie {

    // KOBIS 영화 코드를 기본키로 사용
    @Id
    @Column(length = 20)
    private String movieCode;

    // 영화 기본정보
    private String title;
    private String titleEn;
    private String releaseDate;
    private String showTime;
    private String genre;
    private String director;

    // 출연 배우 이름 목록 저장
    @Column(columnDefinition = "TEXT")
    private String actors;

    private String watchGrade;

    // 박스오피스 진입 이력. 일별 박스오피스 수집 시 누적 갱신한다.
    @Column(name = "box_office_entered", nullable = false)
    private boolean boxOfficeEntered = false;

    // 확인된 박스오피스 최고 순위와 가장 최근 진입일
    @Column(name = "best_box_office_rank")
    private Integer bestBoxOfficeRank;

    @Column(name = "last_box_office_date", length = 8)
    private String lastBoxOfficeDate;

    @Column(name = "last_box_office_rank")
    private Integer lastBoxOfficeRank;

    // 외부 API 상세정보 보완 상태
    private boolean supplementVerified;

    @Column(length = 50)
    private String supplementSources;

    // 마지막 외부 API 보완 시도 시각
    private LocalDateTime supplementAttemptedAt;

    @Column(name = "kobis_detail_attempted_at")
    private LocalDateTime kobisDetailAttemptedAt;

    // 포스터 정보
    @Column(length = 1000)
    private String posterUrl;

    public void markBoxOfficeEntered(int rank, String targetDate) {
        if (rank > 0 && (bestBoxOfficeRank == null || rank < bestBoxOfficeRank)) {
            bestBoxOfficeRank = rank;
        }

        if (targetDate != null && targetDate.matches("\\d{8}")
                && (lastBoxOfficeDate == null || targetDate.compareTo(lastBoxOfficeDate) >= 0)) {
            lastBoxOfficeDate = targetDate;
            lastBoxOfficeRank = rank;
        }

        boxOfficeEntered = true;
    }

    public void updatePosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    // KOBIS에서 가져온 영화 정보로 Movie 객체 생성
    public Movie(String movieCode, String title, String titleEn,
                 String releaseDate, String showTime, String genre,
                 String director, String actors, String watchGrade) {

        this.movieCode = movieCode;
        this.title = title;
        this.titleEn = titleEn;
        this.releaseDate = releaseDate;
        this.showTime = showTime;
        this.genre = genre;
        this.director = director;
        this.actors = actors;
        this.watchGrade = watchGrade;

        this.supplementVerified = false;
        this.supplementSources = "KOBIS";
    }

    // KOBIS에서 누락된 항목만 외부 API 정보로 보완
    public boolean supplementMissingDetails(
            String titleEn, String releaseDate, String showTime, String genre,
            String director, String actors, String watchGrade, String source) {

        boolean changed = false;

        changed |= isFillable(this.titleEn, titleEn);
        changed |= isFillable(this.releaseDate, releaseDate);
        changed |= isFillable(this.showTime, showTime);
        changed |= isFillable(this.genre, genre);
        changed |= isFillable(this.director, director);
        changed |= isFillable(this.actors, actors);
        changed |= isFillable(this.watchGrade, watchGrade);

        this.titleEn = fillMissing(this.titleEn, titleEn);
        this.releaseDate = fillMissing(this.releaseDate, releaseDate);
        this.showTime = fillMissing(this.showTime, showTime);
        this.genre = fillMissing(this.genre, genre);
        this.director = fillMissing(this.director, director);
        this.actors = fillMissing(this.actors, actors);
        this.watchGrade = fillMissing(this.watchGrade, watchGrade);

        if (!changed) {
            return false;
        }

        this.supplementVerified = true;

        if (this.supplementSources == null || this.supplementSources.isBlank()) {
            this.supplementSources = "KOBIS";
        }

        if (source != null && !source.isBlank()
                && !this.supplementSources.contains(source)) {
            this.supplementSources += "," + source;
        }

        return true;
    }

    // 마지막 보완 시도로부터 24시간이 지났는지 확인
    public boolean canAttemptSupplement(LocalDateTime now) {
        return supplementAttemptedAt == null
                || supplementAttemptedAt.isBefore(now.minusHours(24));
    }

    // KMDb/TMDb 외부 보완 시도 시각
    public void markSupplementAttempted(LocalDateTime attemptedAt) {
        this.supplementAttemptedAt = attemptedAt;
    }

    public boolean canAttemptKobisDetail(LocalDateTime now) {
        return kobisDetailAttemptedAt == null
                || kobisDetailAttemptedAt.isBefore(now.minusHours(24));
    }

    public void markKobisDetailAttempted(LocalDateTime attemptedAt) {
        this.kobisDetailAttemptedAt = attemptedAt;
    }

    // 기존 값이 비어 있고 새로운 값이 존재하는지 확인
    private boolean isFillable(String currentValue, String candidateValue) {
        return (currentValue == null || currentValue.isBlank())
                && candidateValue != null && !candidateValue.isBlank();
    }

    // 기존 값이 없을 때만 새로운 값으로 채움
    private String fillMissing(String currentValue, String candidateValue) {
        if (currentValue == null || currentValue.isBlank()) {
            return candidateValue == null ? "" : candidateValue.trim();
        }

        return currentValue;
    }
}