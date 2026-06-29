package com.innowise.paymentservice.infrastructure.security.converter;


import com.innowise.paymentservice.infrastructure.security.model.JwtUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static class JwtClaim {
        private static final String LOGIN = "login";
        private static final String ROLES = "roles";
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(JwtClaim.ROLES);
        log.info("JWT roles claim: {}", jwt.getClaimAsStringList(JwtClaim.ROLES));
        Collection<GrantedAuthority> authorities = roles == null ? List.of() :
                roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        log.debug("Request has the following roles: {}", authorities);
        String userId = jwt.getSubject();
        String login = jwt.getClaimAsString(JwtClaim.LOGIN);


        JwtUserDetails userDetails = new JwtUserDetails(Long.parseLong(userId), login);

        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }
}
