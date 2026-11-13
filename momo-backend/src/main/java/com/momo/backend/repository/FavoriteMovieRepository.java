package com.momo.backend.repository;

import com.momo.backend.entity.FavoriteMovie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteMovieRepository extends JpaRepository<FavoriteMovie, Long> {

    List<FavoriteMovie> findAllByUser_UserIdOrderByCreatedAtDescFavoriteIdDesc(Long userId);

    Optional<FavoriteMovie> findByUser_UserIdAndMovieCode(Long userId, String movieCode);

    @Modifying
    @Query("delete from FavoriteMovie favorite where favorite.user.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from FavoriteMovie favorite where favorite.user.userId = :userId and favorite.movieCode = :movieCode")
    int deleteByUserIdAndMovieCode(
            @Param("userId") Long userId,
            @Param("movieCode") String movieCode
    );
}
