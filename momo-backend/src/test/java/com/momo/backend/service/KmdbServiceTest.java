package com.momo.backend.service;

import com.momo.backend.client.KmdbApiClient;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KmdbServiceTest {

    @Test
    void acceptsExactPosterTitleWhenKmdbReleaseYearAndDirectorAreMissing() {
        KmdbApiClient kmdbApiClient = mock(KmdbApiClient.class);
        TmdbService tmdbService = mock(TmdbService.class);
        KmdbService service = new KmdbService(
                kmdbApiClient,
                tmdbService,
                new ObjectMapper()
        );

        when(kmdbApiClient.searchMovie("동명 영화")).thenReturn("""
                {
                  "Data": [{
                    "Result": [{
                      "DOCID": "DOC-1",
                      "title": "동명 영화",
                      "titleEng": "",
                      "prodYear": "",
                      "repRlsDate": "",
                      "posters": "https://kmdb.example/posters/movie-1.jpg",
                      "directors": {"director": []}
                    }]
                  }]
                }
                """);

        assertEquals(
                "https://kmdb.example/posters/movie-1.jpg",
                service.findPosterUrl("K00000001", "동명 영화", "", "", "")
        );
        verify(tmdbService, never()).findMoviePosterUrl(
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void doesNotChooseFirstPosterWhenSameTitleCandidatesAreEquallyRanked() {
        KmdbApiClient kmdbApiClient = mock(KmdbApiClient.class);
        TmdbService tmdbService = mock(TmdbService.class);
        KmdbService service = new KmdbService(
                kmdbApiClient,
                tmdbService,
                new ObjectMapper()
        );

        when(kmdbApiClient.searchMovie("동명 영화")).thenReturn("""
                {
                  "Data": [{
                    "Result": [
                      {
                        "DOCID": "DOC-REMAKE-1",
                        "title": "동명 영화",
                        "titleEng": "",
                        "prodYear": "",
                        "repRlsDate": "",
                        "posters": "https://kmdb.example/posters/remake-1.jpg",
                        "directors": {"director": []}
                      },
                      {
                        "DOCID": "DOC-REMAKE-2",
                        "title": "동명 영화",
                        "titleEng": "",
                        "prodYear": "",
                        "repRlsDate": "",
                        "posters": "https://kmdb.example/posters/remake-2.jpg",
                        "directors": {"director": []}
                      }
                    ]
                  }]
                }
                """);
        when(tmdbService.findMoviePosterUrl("동명 영화", "", "", "")).thenReturn(null);

        assertNull(service.findPosterUrl("K00000001", "동명 영화", "", "", ""));
        verify(tmdbService).findMoviePosterUrl("동명 영화", "", "", "");
    }
}
