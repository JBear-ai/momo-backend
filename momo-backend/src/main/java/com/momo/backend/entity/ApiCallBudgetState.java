package com.momo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "api_call_budget")
public class ApiCallBudgetState {

    @Id
    @Column(length = 100)
    private String id;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(nullable = false)
    private LocalDate usageDate;

    @Column(nullable = false)
    private long prefetchCalls;

    @Column(nullable = false)
    private long demandCalls;

    public static ApiCallBudgetState create(String provider, LocalDate date) {
        ApiCallBudgetState state = new ApiCallBudgetState();
        state.id = provider + "_" + date;
        state.provider = provider;
        state.usageDate = date;
        return state;
    }

    public void increasePrefetch() {
        prefetchCalls++;
    }

    public void increaseDemand() {
        demandCalls++;
    }
}
