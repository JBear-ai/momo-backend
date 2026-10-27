package com.momo.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Slf4j
@RestControllerAdvice // 모든 Controller에서 발생하는 예외를 공통으로 처리
public class GlobalExceptionHandler {

    // 1. 사용자 정의 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException error) {

        HttpStatus status;

        // 오류 코드에 따라 HTTP 상태 결정
        switch (error.getCode()) {
            case "USER_NOT_FOUND":
            case "REVIEW_NOT_FOUND":
            case "COMMENT_NOT_FOUND":
            case "MOVIE_NOT_FOUND":
                status = HttpStatus.NOT_FOUND;
                break;

            case "DUPLICATE_LOGIN_ID":
            case "DUPLICATE_NICKNAME":
            case "DUPLICATE_REVIEW":
                status = HttpStatus.CONFLICT;
                break;

            case "UNAUTHORIZED":
                status = HttpStatus.UNAUTHORIZED;
                break;

            case "FORBIDDEN":
                status = HttpStatus.FORBIDDEN;
                break;

            default:
                status = HttpStatus.BAD_REQUEST;
                break;
        }

        return ResponseEntity.status(status)
                .body(new ErrorResponse(error.getCode(), error.getMessage()));
    }

    // 2. 필수 요청 파라미터 누락 처리
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException error) {

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        "MISSING_PARAM",
                        error.getParameterName() + " 파라미터가 누락되었습니다."
                ));
    }

    // 3. 잘못된 요청값 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException error) {

        log.warn("잘못된 요청: {}", error.getMessage());

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        "INVALID_REQUEST",
                        error.getMessage()
                ));
    }

    // 4. 데이터베이스 접근 오류 처리
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDBError(DataAccessException error) {

        log.error("데이터베이스 오류 발생", error);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "DB_ERROR",
                        "데이터베이스 오류가 발생했습니다."
                ));
    }

    // 5. 그 외 예상하지 못한 서버 오류 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleDefault(Exception error) {

        log.error("서버 오류 발생", error);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "INTERNAL_SERVER_ERROR",
                        "서버 내부 오류가 발생했습니다."
                ));
    }

    // 6. DTO 입력값 검증 실패 처리 (@NotBlank, @Email 등)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException error) {

        // 검증에 실패한 첫 번째 필드의 오류 메시지 가져오기
        String message = error.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");

        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", message));
    }

}