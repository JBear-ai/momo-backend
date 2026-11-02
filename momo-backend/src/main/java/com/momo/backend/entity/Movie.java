package com.momo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private String title;        // 영화 제목
    private String titleEn;      // 영문 제목
    private String releaseDate;  // 개봉일
    private String showTime;     // 상영 시간
    private String genre;        // 장르
    private String director;     // 감독

    // 출연 배우 이름 목록 저장
    @Column(columnDefinition = "TEXT")
    private String actors;

    private String watchGrade;   // 관람 등급

    @Column(length = 1000)
    private String posterUrl;
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
    }
}