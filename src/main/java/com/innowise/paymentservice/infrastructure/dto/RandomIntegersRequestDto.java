package com.innowise.paymentservice.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RandomIntegersRequestDto(
        @JsonProperty("jsonrpc") double jsonRpcApiVersion,
        String method,
        Params params,
        @JsonProperty("id") Long requestId

) {

    public record Params(
            String apiKey,
            @JsonProperty("n") int amtIntegersRequested
    ) {}
}
