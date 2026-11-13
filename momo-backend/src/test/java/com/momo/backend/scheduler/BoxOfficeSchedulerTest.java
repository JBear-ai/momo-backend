package com.momo.backend.scheduler;

import com.momo.backend.service.MovieService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.ResourceAccessException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoxOfficeSchedulerTest {

    @Mock
    private MovieService movieService;

    @InjectMocks
    private BoxOfficeScheduler boxOfficeScheduler;

    @Test
    void keepsApplicationStartupAliveWhenKobisIsUnavailable() {
        when(movieService.getDailyBoxOffice(anyString()))
                .thenThrow(new ResourceAccessException("KOBIS unavailable"));

        assertDoesNotThrow(boxOfficeScheduler::checkBoxOfficeOnStartup);

        verify(movieService).getDailyBoxOffice(anyString());
    }

    @Test
    void keepsScheduledRefreshAliveWhenKobisIsUnavailable() {
        when(movieService.getDailyBoxOffice(anyString()))
                .thenThrow(new ResourceAccessException("KOBIS unavailable"));

        assertDoesNotThrow(boxOfficeScheduler::updateDailyBoxOffice);

        verify(movieService).getDailyBoxOffice(anyString());
    }
}
