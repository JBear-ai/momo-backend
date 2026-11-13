package com.momo.backend.client;

import com.momo.backend.service.ApiCallBudgetService;
import com.momo.backend.service.ApiCallMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static com.momo.backend.service.ApiCallBudgetService.TrafficType.PREFETCH;
import static com.momo.backend.service.ApiCallBudgetService.TrafficType.USER_SEARCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

class TmdbApiClientTest {

    @Test
    void chargesTheBudgetForEveryRetryAttempt() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.themoviedb.org/3");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ApiCallBudgetService budget = mock(ApiCallBudgetService.class);
        TmdbApiClient client = new TmdbApiClient(
                builder.build(),
                "test-api-key",
                new ApiCallMetrics(),
                budget
        );

        server.expect(requestTo(containsString("/search/movie")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .header("Retry-After", "0"));
        server.expect(requestTo(containsString("/search/movie")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertEquals("{}", client.searchMovie("Alien", "en-US", USER_SEARCH));

        verify(budget, times(2)).acquire("TMDB", USER_SEARCH);
        server.verify();
    }

    @Test
    void invalidInputDoesNotConsumeApiBudget() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.themoviedb.org/3");
        ApiCallBudgetService budget = mock(ApiCallBudgetService.class);
        TmdbApiClient client = new TmdbApiClient(
                builder.build(),
                "test-api-key",
                new ApiCallMetrics(),
                budget
        );

        assertThrows(IllegalArgumentException.class,
                () -> client.searchMovie(" ", "ko-KR", PREFETCH));

        verifyNoInteractions(budget);
    }
}
