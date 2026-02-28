package at.technikum.springrestbackend.security;

import at.technikum.springrestbackend.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig (unit)")
class SecurityConfigTest {

    @Mock
    private CustomUserDetailsService customUserDetailsService;
    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Mock
    private JwtAuthEntryPoint jwtAuthEntryPoint;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    @DisplayName("passwordEncoder() returns a BCryptPasswordEncoder")
    void passwordEncoderIsBCrypt() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        // Verify it actually encodes — round-trips a value
        String encoded = encoder.encode("secret");
        assertThat(encoder.matches("secret", encoded)).isTrue();
    }

    @Test
    @DisplayName("authenticationProvider() is configured with userDetailsService and passwordEncoder")
    void authenticationProviderIsConfigured() {
        DaoAuthenticationProvider provider = securityConfig.authenticationProvider();

        assertThat(provider).isNotNull();
    }

    @Test
    @DisplayName("authenticationManager() delegates to AuthenticationConfiguration")
    void authenticationManagerDelegatesToConfiguration() throws Exception {
        AuthenticationConfiguration config = mock(AuthenticationConfiguration.class);
        AuthenticationManager mockManager = mock(AuthenticationManager.class);
        when(config.getAuthenticationManager()).thenReturn(mockManager);

        AuthenticationManager result = securityConfig.authenticationManager(config);

        assertThat(result).isSameAs(mockManager);
    }

    @Test
    @DisplayName("jwtFilterRegistration() creates a disabled registration bean")
    void jwtFilterRegistrationIsDisabled() {
        FilterRegistrationBean<JwtAuthenticationFilter> bean =
                securityConfig.jwtFilterRegistration(jwtAuthenticationFilter);

        assertThat(bean).isNotNull();
        assertThat(bean.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("corsConfigurationSource() allows all origins, required methods and headers")
    void corsConfigurationSourceIsConfigured() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        assertThat(source).isInstanceOf(UrlBasedCorsConfigurationSource.class);

        CorsConfiguration config =
                ((UrlBasedCorsConfigurationSource) source).getCorsConfiguration(
                        new org.springframework.mock.web.MockHttpServletRequest());

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOriginPatterns()).containsExactly("*");
        assertThat(config.getAllowedMethods())
                .containsExactlyInAnyOrderElementsOf(
                        List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        assertThat(config.getAllowedHeaders()).containsExactly("*");
        assertThat(config.getAllowCredentials()).isFalse();
        assertThat(config.getExposedHeaders()).containsExactly("Authorization");
    }
}