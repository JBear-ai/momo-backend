
package com.momo.backend.entity;

// 포스터 조회 진행 상태
public enum PosterStatus {

    // 조회 대기
    PENDING,

    // 조회 진행 중
    PROCESSING,

    // 조회 성공
    SUCCESS,

    // 조회 실패
    FAILED
}
