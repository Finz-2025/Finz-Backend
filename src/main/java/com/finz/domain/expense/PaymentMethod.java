package com.finz.domain.expense;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    CARD("카드"),
    CASH("현금"),
    BANK_TRANSFER("계좌이체");

    private final String description;

    // Request의 결제수단 문자열을 Enum 상수로 변경
    public static PaymentMethod fromDescription(String description) {
        return Arrays.stream(PaymentMethod.values())
                .filter(method -> method.getDescription().equals(description))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 결제수단입니다: " + description));
    }
}
