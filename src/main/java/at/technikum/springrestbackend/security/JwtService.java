package at.technikum.springrestbackend.security;

import at.technikum.springrestbackend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final String jwtSecret;
    private final long jwtExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") final String jwtSecret,
            @Value("${app.jwt.expiration-ms}") final long jwtExpirationMs
    ) {
        this.jwtSecret = jwtSecret;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String generateToken(final User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Long extractUserId(final String token) {
        Claims claims = extractAllClaims(token);
        String subject = claims.getSubject();

        if (subject == null || subject.isBlank()) {
            return null;
        }

        return Long.parseLong(subject);
    }

    public String extractEmail(final String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("email", String.class);
    }

    public Date extractExpiration(final String token) {
        return extractAllClaims(token).getExpiration();
    }

    public boolean isTokenValid(final String token) {
        return !isTokenExpired(token);
    }

    public boolean isTokenValidForUser(
            final String token,
            final CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return false;
        }

        Long tokenUserId = extractUserId(token);
        String tokenEmail = extractEmail(token);

        boolean sameUserId = tokenUserId != null && tokenUserId.equals(userDetails.getId());
        boolean sameEmail = tokenEmail != null && tokenEmail.equals(userDetails.getEmail());

        return sameUserId && sameEmail && !isTokenExpired(token);
    }

    private boolean isTokenExpired(final String token) {
        Date expiration = extractExpiration(token);
        return expiration.before(new Date());
    }

    private Claims extractAllClaims(final String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }
}