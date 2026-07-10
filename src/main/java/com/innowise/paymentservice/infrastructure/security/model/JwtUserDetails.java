package com.innowise.paymentservice.infrastructure.security.model;


import com.innowise.paymentservice.application.security.model.UserContext;

public record JwtUserDetails(
        Long userId,
        String login
) implements UserContext {
}
