package com.momo.backend.service;

import com.momo.backend.client.TmdbApiClient;
import com.momo.backend.dto.movie.TmdbMovieDetailResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TmdbServiceMovieDetailTest {

    @Test
    void loadsMovieDetailsAndPersonIdsDirectlyFromTmdbId() {
        TmdbApiClient client = mock(TmdbApiClient.class);
        when(client.getMovieDetail(555094L)).thenReturn("""
                {
                  "id": 555094,
                  "title": "劇場版 薬屋のひとりごと 亡妃の秘宝",
                  "original_title": "The Apothecary Diaries Movie",
                  "release_date": "2026-12-11",
                  "runtime": 112,
                  "poster_path": "/poster.jpg",
                  "overview": "TMDb overview",
                  "genres": [{"name": "Animation"}, {"name": "Mystery"}],
                  "release_dates": {
                    "results": [{
                      "iso_3166_1": "KR",
                      "release_dates": [{
                        "type": 3,
                        "release_date": "2026-12-11T00:00:00.000Z",
                        "certification": "12"
                      }]
                    }]
                  },
                  "credits": {
                    "crew": [{
                      "id": 10,
                      "name": "Director Name",
                      "original_name": "Director Name",
                      "profile_path": "/director.jpg",
                      "job": "Director"
                    }],
                    "cast": [{
                      "id": 20,
                      "name": "Actor Name",
                      "original_name": "Actor Name",
                      "profile_path": "/actor.jpg",
                      "character": "Main Character"
                    }]
                  }
                }
                """);

        TmdbService service = new TmdbService(client, new ObjectMapper());

        TmdbMovieDetailResponse detail = service.getMovieDetailById(555094L);

        assertEquals(555094L, detail.tmdbId());
        assertNull(detail.movieCode());
        assertEquals("劇場版 薬屋のひとりごと 亡妃の秘宝", detail.title());
        assertEquals("20261211", detail.releaseDate());
        assertEquals("112", detail.runningTime());
        assertEquals("Animation, Mystery", detail.genre());
        assertEquals("Director Name", detail.director());
        assertEquals("12", detail.watchGrade());
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", detail.posterUrl());
        assertEquals("Actor Name", detail.actors().get(0));
        assertEquals(2, detail.people().size());
        assertEquals(10L, ((Number) detail.people().get(0).get("personId")).longValue());
        assertEquals(20L, ((Number) detail.people().get(1).get("personId")).longValue());
        verify(client).getMovieDetail(555094L);
    }

    @Test
    void rejectsInvalidTmdbIdBeforeCallingExternalApi() {
        TmdbApiClient client = mock(TmdbApiClient.class);
        TmdbService service = new TmdbService(client, new ObjectMapper());

        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.getMovieDetailById(0L));
        verifyNoInteractions(client);
    }
}
