package com.momo.backend.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

/** Unicode 정규화 후 금칙어의 부분 문자열 포함 여부를 검사한다. */
@Component
public class ChatProfanityFilter {

    private final ChatBannedWords bannedWords;

    public ChatProfanityFilter(ChatBannedWords bannedWords) {
        this.bannedWords = bannedWords;
    }

    /** 정규화된 메시지에 관리 목록의 금칙어가 포함됐는지 확인한다. */
    public boolean containsBannedWord(String content) {
        String normalizedContent = normalize(content);
        return bannedWords.getWords().stream()
                .map(ChatProfanityFilter::normalize)
                .anyMatch(normalizedContent::contains);
    }

    private static String normalize(String value) {
        // 호환 유니코드 표기와 대소문자 차이를 줄여 단순 우회 표현을 일부 차단한다.
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }
}
