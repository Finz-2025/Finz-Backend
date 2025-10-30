package com.finz.dto.home;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HomeHighlightResponseDto {

    @JsonProperty("category_highlight")
    private final HighlightItemDto categoryHighlight;

    @JsonProperty("tag_highlight")
    private final HighlightItemDto tagHighlight;

    @JsonProperty("recommend_spend")
    private final HighlightItemDto recommendSpend;
}
