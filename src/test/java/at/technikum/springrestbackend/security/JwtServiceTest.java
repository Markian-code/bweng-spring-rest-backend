package at.technikum.springrestbackend.security;

import at.technikum.springrestbackend.entity.Role;
import at.technikum.springrestbackend.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService")
class JwtServiceTest {

    // HS256 requires a key of at least 256 bits (32 bytes)
    private static final String SECRET =
            "test-secret-key-that-is-at-least-32-bytes!!";
    private static final long EXPIRATION_MS = 60_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    // fixture helpers

    private User buildUser(Long id, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername("user_" + id);
        user.setPasswordHash("hashedpw");
        user.setCountryCode("AT");
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private CustomUserDetails buildUserDetails(User user) {
        return new CustomUserDetails(user);
    }

    //  generateToken

    @Nested
    @DisplayName("generateToken(User)")
    class GenerateToken {

        @Test
        @DisplayName("subject equals user ID (round-trip via extractUserId)")
        void subjectIsUserId() {
            User user = buildUser(42L, "bob@example.com", Role.USER);
            String token = jwtService.generateToken(user);

            assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        }

        @Test
        @DisplayName("email claim matches user email")
        void emailClaimMatchesUser() {
            User user = buildUser(1L, "alice@example.com", Role.USER);
            String token = jwtService.generateToken(user);

            assertThat(jwtService.extractEmail(token)).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("expiration is approximately now + expirationMs")
        void expirationIsInFuture() {
            User user = buildUser(1L, "alice@example.com", Role.USER);
            long before = System.currentTimeMillis();
            String token = jwtService.generateToken(user);
            long after = System.currentTimeMillis();

            Date expiration = jwtService.extractExpiration(token);

            assertThat(expiration.getTime())
                    .isBetween(before + EXPIRATION_MS - 1000, after + EXPIRATION_MS + 1000);
        }

        @Test
        @DisplayName("ADMIN user token is parseable and carries correct userId and email")
        void adminTokenIsValid() {
            User admin = buildUser(7L, "admin@example.com", Role.ADMIN);
            String token = jwtService.generateToken(admin);

            assertThat(jwtService.extractUserId(token)).isEqualTo(7L);
            assertThat(jwtService.extractEmail(token)).isEqualTo("admin@example.com");
        }
    }

    //  extractUserId

    @Nested
    @DisplayName("extractUserId(String)")
    class ExtractUserId {

        @Test
        @DisplayName("returns correct Long from token subject")
        void returnsUserId() {
            User user = buildUser(99L, "test@example.com", Role.USER);
            String token = jwtService.generateToken(user);

            assertThat(jwtService.extractUserId(token)).isEqualTo(99L);
        }
    }

    //  extractEmail

    @Nested
    @DisplayName("extractEmail(String)")
    class ExtractEmail {

        @Test
        @DisplayName("returns email claim value from token")
        void returnsEmail() {
            User user = buildUser(1L, "extract@example.com", Role.USER);
            String token = jwtService.generateToken(user);

            assertThat(jwtService.extractEmail(token)).isEqualTo("extract@example.com");
        }
    }

    //  isTokenValid

    @Nested
    @DisplayName("isTokenValid(String)")
    class IsTokenValid {

        @Test
        @DisplayName("returns true for a freshly generated token")
        void trueForFreshToken() {
            User user = buildUser(1L, "test@example.com", Role.USER);
            String token = jwtService.generateToken(user);

            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("EDGE: throws ExpiredJwtException for expired token (does not return false)")
        void throwsForExpiredToken() {
            JwtService expiredJwtService = new JwtService(SECRET, -1000L);
            User user = buildUser(1L, "test@example.com", Role.USER);
            String expiredToken = expiredJwtService.generateToken(user);

            assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }

    //  isTokenValidForUser

    @Nested
    @DisplayName("isTokenValidForUser(String, CustomUserDetails)")
    class IsTokenValidForUser {

        @Test
        @DisplayName("returns true when userId and email both match the token")
        void trueWhenBothMatch() {
            User user = buildUser(5L, "user@example.com", Role.USER);
            String token = jwtService.generateToken(user);
            CustomUserDetails details = buildUserDetails(user);

            assertThat(jwtService.isTokenValidForUser(token, details)).isTrue();
        }

        @Test
        @DisplayName("returns false immediately when userDetails is null")
        void falseWhenUserDetailsIsNull() {
            User user = buildUser(5L, "user@example.com", Role.USER);
            String token = jwtService.generateToken(user);

            assertThat(jwtService.isTokenValidForUser(token, null)).isFalse();
        }

        @Test
        @DisplayName("returns false when userId in token does not match userDetails id")
        void falseWhenUserIdMismatch() {
            User tokenOwner = buildUser(5L, "user@example.com", Role.USER);
            User otherUser = buildUser(99L, "user@example.com", Role.USER);
            String token = jwtService.generateToken(tokenOwner);
            CustomUserDetails details = buildUserDetails(otherUser);

            assertThat(jwtService.isTokenValidForUser(token, details)).isFalse();
        }

        @Test
        @DisplayName("returns false when email in token does not match userDetails email")
        void falseWhenEmailMismatch() {
            User tokenOwner = buildUser(5L, "original@example.com", Role.USER);
            User changedEmail = buildUser(5L, "changed@example.com", Role.USER);
            String token = jwtService.generateToken(tokenOwner);
            CustomUserDetails details = buildUserDetails(changedEmail);

            assertThat(jwtService.isTokenValidForUser(token, details)).isFalse();
        }

        @Test
        @DisplayName("EDGE: throws ExpiredJwtException when token is expired (not graceful false)")
        void throwsForExpiredToken() {
            JwtService expiredJwtService = new JwtService(SECRET, -1000L);
            User user = buildUser(5L, "user@example.com", Role.USER);
            String expiredToken = expiredJwtService.generateToken(user);
            CustomUserDetails details = buildUserDetails(user);

            assertThatThrownBy(() -> jwtService.isTokenValidForUser(expiredToken, details))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }

    //  getJwtExpirationMs

    @Nested
    @DisplayName("getJwtExpirationMs()")
    class GetJwtExpirationMs {

        @Test
        @DisplayName("returns the configured expiration value")
        void returnsConfiguredValue() {
            assertThat(jwtService.getJwtExpirationMs()).isEqualTo(EXPIRATION_MS);
        }
    }
}