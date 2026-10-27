package com.momo.backend.exception;

// 오류 코드와 메시지를 JSON으로 반환하는 DTO
// 기존에는 @Getter, @AllArgsConstructor를 사용했지만 Java 21의 record로 대체
// record는 생성자, 값 조회 메서드, equals(), hashCode(), toString()을 자동 생성
// Lombok의 getCode(), getMessage() 대신 code(), message()로 값을 조회
// record는 생성 후 필드 값을 변경할 수 없는 불변 데이터 객체
public record ErrorResponse(
        String code,       // 오류 코드
        String message     // 오류 메시지
) {
}