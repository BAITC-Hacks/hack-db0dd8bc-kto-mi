package com.qadam.dto;

import com.qadam.model.Industry;

public record IndustryResponse(String code, String name) {

    public static IndustryResponse of(Industry industry) {
        return new IndustryResponse(industry.name(), industry.getDisplayName());
    }
}
