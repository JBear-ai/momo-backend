package com.momo.backend.service;

import org.springframework.stereotype.Component;

import java.util.List;

/** 금칙어 목록만 관리해 메시지 처리 코드와 분리한다. */
@Component
public class ChatBannedWords {

    private final List<String> words = List.of(
            "시발", "씨발", "ㅅㅂ", "ㅆㅂ", "병신", "개새끼", "좆", "존나", "지랄", "염병");

    public List<String> getWords() {
        return words;
    }
}
