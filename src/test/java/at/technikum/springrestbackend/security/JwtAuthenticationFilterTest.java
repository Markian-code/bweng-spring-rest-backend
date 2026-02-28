package at.technikum.springrestbackend.security;

import at.technikum.springrestbackend.entity.Role;
import at.technikum.springrestbackend.entity.User;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---- helpers ----

    private CustomUserDetails buildDetails(Long id, String email, boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setCountryCode("AT");
        user.setRole(Role.USER);
        user.setEnabled(enabled);
        return new CustomUserDetails(user);
    }

    // extractJwtFromRequest — header parsing (L79-L86)

    @Nested
    @DisplayName("extractJwtFromRequest — header parsing")
    class ExtractJwtFromRequest {

        @Test
        @DisplayName("no Authorization header → null token → chain proceeds, no JWT work")
        void noHeader() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("header present but not 'Bearer ' prefix → null token, chain proceeds")
        void headerWithWrongPrefix() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("'Bearer ' header with only whitespace after prefix → blank token → null returned")
        void bearerPrefixWithBlankToken() throws Exception {
            // "Bearer " + 3 spaces — substring(7) = "   " → hasText = false → returns null
            when(request.getHeader("Authorization")).thenReturn("Bearer    ");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService);
        }
    }

    // doFilterInternal — already-authenticated fast-path (L43)

    @Nested
    @DisplayName("doFilterInternal — SecurityContext already populated")
    class AlreadyAuthenticated {

        @Test
        @DisplayName("token present but SecurityContext already holds auth → filter body skipped")
        void skipsProcessingWhenAlreadyAuthenticated() throws Exception {
            Authentication existing = mock(Authentication.class);
            SecurityContextHolder.getContext().setAuthentication(existing);
            when(request.getHeader("Authorization")).thenReturn("Bearer some.valid.token");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            // JWT service must NOT be consulted — auth was already set
            verifyNoInteractions(jwtService);
        }
    }

    // doFilterInternal — token processing (L45-L72)

    @Nested
    @DisplayName("doFilterInternal — token processing")
    class TokenProcessing {

        @Test
        @DisplayName("JwtException from extractUserId → SecurityContext cleared, chain still proceeds")
        void jwtExceptionClearsContextAndContinues() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer bad.jwt");
            when(jwtService.extractUserId("bad.jwt"))
                    .thenThrow(new JwtException("invalid signature"));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("IllegalArgumentException from extractUserId → SecurityContext cleared, chain proceeds")
        void illegalArgExceptionClearsContextAndContinues() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer malformed");
            when(jwtService.extractUserId("malformed"))
                    .thenThrow(new IllegalArgumentException("malformed claims"));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("extractUserId returns null → inner block skipped, no authentication set")
        void nullUserIdSkipsAuthentication() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer anon.token");
            when(jwtService.extractUserId("anon.token")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(customUserDetailsService);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("userDetails is not CustomUserDetails → authentication not set")
        void nonCustomUserDetailsSkipsAuthentication() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer valid.token");
            when(jwtService.extractUserId("valid.token")).thenReturn(1L);
            UserDetails plainDetails = mock(UserDetails.class);
            when(customUserDetailsService.loadUserById(1L)).thenReturn(plainDetails);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("isTokenValidForUser returns false (expired/tampered) → authentication not set")
        void invalidTokenSkipsAuthentication() throws Exception {
            CustomUserDetails details = buildDetails(1L, "user@test.com", true);
            when(request.getHeader("Authorization")).thenReturn("Bearer expired.token");
            when(jwtService.extractUserId("expired.token")).thenReturn(1L);
            when(customUserDetailsService.loadUserById(1L)).thenReturn(details);
            when(jwtService.isTokenValidForUser("expired.token", details)).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("token valid but user is disabled → authentication not set")
        void disabledUserSkipsAuthentication() throws Exception {
            CustomUserDetails details = buildDetails(1L, "user@test.com", false);
            when(request.getHeader("Authorization")).thenReturn("Bearer valid.token");
            when(jwtService.extractUserId("valid.token")).thenReturn(1L);
            when(customUserDetailsService.loadUserById(1L)).thenReturn(details);
            when(jwtService.isTokenValidForUser("valid.token", details)).thenReturn(true);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("valid token + enabled user → authentication set in SecurityContext")
        void validTokenAndEnabledUserSetsAuthentication() throws Exception {
            CustomUserDetails details = buildDetails(1L, "user@test.com", true);
            when(request.getHeader("Authorization")).thenReturn("Bearer valid.token");
            when(jwtService.extractUserId("valid.token")).thenReturn(1L);
            when(customUserDetailsService.loadUserById(1L)).thenReturn(details);
            when(jwtService.isTokenValidForUser("valid.token", details)).thenReturn(true);
            // WebAuthenticationDetailsSource reads these from the request
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getSession(false)).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getPrincipal()).isEqualTo(details);
            assertThat(auth.getAuthorities()).isEqualTo(details.getAuthorities());
        }
    }
}