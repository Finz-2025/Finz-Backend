package com.finz.dto.home;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HighlightItemDto {
    private final String title;    // "식비", "배달" 등

    @JsonProperty("value_text")
    private final String valueText; // "-18%", "1회 증가" 등
}
