package com.momo.backend.service;

import com.momo.backend.dto.favorite.FavoriteMovieRequest;
import com.momo.backend.dto.favorite.FavoriteMovieResponse;
import com.momo.backend.entity.FavoriteMovie;
import com.momo.backend.entity.User;
import com.momo.backend.repository.FavoriteMovieRepository;
import com.momo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteMovieService {

    private final FavoriteMovieRepository favoriteMovieRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<FavoriteMovieResponse> getFavorites(String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        return favoriteMovieRepository
                .findAllByUser_UserIdOrderByCreatedAtDescFavoriteIdDesc(user.getUserId())
                .stream()
                .map(FavoriteMovieResponse::new)
                .toList();
    }

    public FavoriteMovieResponse addFavorite(
            String loginId,
            FavoriteMovieRequest request
    ) {
        User user = userRepository.findByLoginIdForUpdate(loginId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        FavoriteMovie favorite = favoriteMovieRepository
                .findByUser_UserIdAndMovieCode(user.getUserId(), request.movieCode())
                .orElse(null);

        if (favorite == null) {
            favorite = favoriteMovieRepository.saveAndFlush(
                    new FavoriteMovie(
                            user,
                            request.movieCode(),
                            request.title(),
                            request.releaseDate(),
                            request.posterUrl()
                    )
            );
        } else {
            favorite.updateMovieSnapshot(
                    request.title(),
                    request.releaseDate(),
                    request.posterUrl()
            );
        }

        return new FavoriteMovieResponse(favorite);
    }

    public void removeFavorite(String loginId, String movieCode) {
        User user = userRepository.findByLoginIdForUpdate(loginId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        // 이미 삭제된 관심 영화를 다시 삭제해도 성공으로 처리해 요청 재시도에 안전하게 만든다.
        favoriteMovieRepository.deleteByUserIdAndMovieCode(user.getUserId(), movieCode);
    }
}
