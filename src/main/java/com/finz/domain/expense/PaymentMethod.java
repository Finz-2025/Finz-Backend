package com.finz.domain.expense;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    CARD("카드"),
    CASH("현금"),
    BANK_TRANSFER("계좌이체");

    private final String description;
}
