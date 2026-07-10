package com.innowise.paymentservice.infrastructure.dto;

import java.util.List;

public record RandomIntegersResponseDto(
        Result result
) {
    public record Result(
            Random random
    ) {
        public record Random(
                List<Integer> data
        ) {}
    }
}
