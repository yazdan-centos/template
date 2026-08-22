package org.example.template.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Profile("!no-security")
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration expiration;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expiration}") Duration expiration
    ) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expiration = expiration;
    }

    public Token createToken(UserDetails user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiration);
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getUsername())
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Token(value, expiration.toSeconds());
    }

    public record Token(String value, long expiresIn) {
    }
}
