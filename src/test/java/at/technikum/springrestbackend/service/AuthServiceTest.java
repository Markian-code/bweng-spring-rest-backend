package at.technikum.springrestbackend.service;

import at.technikum.springrestbackend.dto.AuthRequestDto;
import at.technikum.springrestbackend.dto.AuthResponseDto;
import at.technikum.springrestbackend.dto.RegisterRequestDto;
import at.technikum.springrestbackend.entity.Role;
import at.technikum.springrestbackend.entity.User;
import at.technikum.springrestbackend.exception.BadRequestException;
import at.technikum.springrestbackend.exception.ForbiddenOperationException;
import at.technikum.springrestbackend.repository.UserRepository;
import at.technikum.springrestbackend.security.CustomUserDetails;
import at.technikum.springrestbackend.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    // fixture helpers

    private RegisterRequestDto buildRegisterRequest(
            String email, String username, String password, String countryCode
    ) {
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setEmail(email);
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setCountryCode(countryCode);
        return dto;
    }

    private RegisterRequestDto validRegisterRequest() {
        return buildRegisterRequest("alice@example.com", "alice99", "Secret1!", "AT");
    }

    private AuthRequestDto buildLoginRequest(String email, String password) {
        AuthRequestDto dto = new AuthRequestDto();
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    private User buildSavedUser(Long id, String email, String username, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash("$2a$encoded");
        user.setCountryCode("AT");
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private Authentication mockAuthenticationWith(CustomUserDetails details) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(details);
        return auth;
    }

    //  register

    @Nested
    @DisplayName("register(RegisterRequestDto)")
    class Register {

        @Test
        @DisplayName("creates user with correct fields and returns JWT response")
        void registersUserSuccessfully() {
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(userRepository.existsByUsername("alice99")).thenReturn(false);
            when(passwordEncoder.encode("Secret1!")).thenReturn("$2a$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(jwtService.generateToken(any())).thenReturn("jwt-token");
            when(jwtService.getJwtExpirationMs()).thenReturn(3600000L);

            AuthResponseDto result = authService.register(validRegisterRequest());

            assertThat(result.getAccessToken()).isEqualTo("jwt-token");
            assertThat(result.getTokenType()).isEqualTo("Bearer");
            assertThat(result.getExpiresInMs()).isEqualTo(3600000L);
            assertThat(result.getEmail()).isEqualTo("alice@example.com");
            assertThat(result.getUsername()).isEqualTo("alice99");
            assertThat(result.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("new user gets ROLE_USER, enabled=true and default profile picture")
        void newUserHasCorrectDefaults() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(jwtService.generateToken(any())).thenReturn("token");
            when(jwtService.getJwtExpirationMs()).thenReturn(3600000L);

            authService.register(validRegisterRequest());

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();

            assertThat(saved.getRole()).isEqualTo(Role.USER);
            assertThat(saved.isEnabled()).isTrue();
            assertThat(saved.getProfilePictureUrl()).isEqualTo("/images/profile-placeholder.png");
            assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        }

        @Test
        @DisplayName("email is normalized to lowercase and trimmed before save")
        void normalizesEmail() {
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(jwtService.generateToken(any())).thenReturn("token");
            when(jwtService.getJwtExpirationMs()).thenReturn(3600000L);

            authService.register(buildRegisterRequest(
                    "  ALICE@EXAMPLE.COM  ", "alice99", "Secret1!", "AT"
            ));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
            verify(userRepository).existsByEmail("alice@example.com");
        }

        @Test
        @DisplayName("countryCode is trimmed and uppercased before save")
        void normalizesCountryCode() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(jwtService.generateToken(any())).thenReturn("token");
            when(jwtService.getJwtExpirationMs()).thenReturn(3600000L);

            authService.register(buildRegisterRequest(
                    "alice@example.com", "alice99", "Secret1!", "  at  "
            ));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getCountryCode()).isEqualTo("AT");
        }

        @Test
        @DisplayName("throws BadRequestException when dto is null")
        void throwsForNullDto() {
            assertThatThrownBy(() -> authService.register(null))
                    .isInstanceOf(BadRequestException.class);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BadRequestException when email is null")
        void throwsForNullEmail() {
            assertThatThrownBy(() ->
                    authService.register(buildRegisterRequest(null, "alice99", "Secret1!", "AT")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Email");
        }

        @ParameterizedTest(name = "email=\"{0}\"")
        @ValueSource(strings = {"", "   "})
        @DisplayName("throws BadRequestException when email is blank")
        void throwsForBlankEmail(String blank) {
            assertThatThrownBy(() ->
                    authService.register(buildRegisterRequest(blank, "alice99", "Secret1!", "AT")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Email");
        }

        @Test
        @DisplayName("throws BadRequestException when email is already in use")
        void throwsWhenEmailTaken() {
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(validRegisterRequest()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Email");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BadRequestException when username is null")
        void throwsForNullUsername() {
            assertThatThrownBy(() ->
                    authService.register(buildRegisterRequest(
                            "alice@example.com", null, "Secret1!", "AT")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Username");
        }

        @ParameterizedTest(name = "username=\"{0}\"")
        @ValueSource(strings = {"", "   "})
        @DisplayName("throws BadRequestException when username is blank")
        void throwsForBlankUsername(String blank) {
            assertThatThrownBy(() ->
                    authService.register(buildRegisterRequest(
                            "alice@example.com", blank, "Secret1!", "AT")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Username");
        }

        @Test
        @DisplayName("throws BadRequestException when username is already in use")
        void throwsWhenUsernameTaken() {
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(userRepository.existsByUsername("alice99")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(validRegisterRequest()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Username");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BadRequestException when countryCode is null")
        void throwsForNullCountryCode() {
            assertThatThrownBy(() ->
                    authService.register(buildRegisterRequest(
                            "alice@example.com", "alice99", "Secret1!", null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Country");
        }

        @ParameterizedTest(name = "countryCode=\"{0}\"")
        @ValueSource(strings = {"", "   "})
        @DisplayName("throws BadRequestException when countryCode is blank")
        void throwsForBlankCountryCode(String blank) {
            assertThatThrownBy(() ->
                    authService.register(buildRegisterRequest(
                            "alice@example.com", "alice99", "Secret1!", blank)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Country");
        }
    }

   //  login

    @Nested
    @DisplayName("login(AuthRequestDto)")
    class Login {

        @Test
        @DisplayName("authenticates user and returns JWT response")
        void loginSuccessfully() {
            User user = buildSavedUser(1L, "alice@example.com", "alice99", Role.USER);
            CustomUserDetails details = new CustomUserDetails(user);
            Authentication auth = mockAuthenticationWith(details);

            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(jwtService.generateToken(user)).thenReturn("jwt-token");
            when(jwtService.getJwtExpirationMs()).thenReturn(3600000L);

            AuthResponseDto result = authService.login(
                    buildLoginRequest("alice@example.com", "Secret1!")
            );

            assertThat(result.getAccessToken()).isEqualTo("jwt-token");
            assertThat(result.getTokenType()).isEqualTo("Bearer");
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("alice@example.com");
            assertThat(result.getUsername()).isEqualTo("alice99");
            assertThat(result.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("email is normalized (trimmed + lowercased) before authentication")
        void normalizesEmailBeforeAuth() {
            User user = buildSavedUser(1L, "alice@example.com", "alice99", Role.USER);
            CustomUserDetails details = new CustomUserDetails(user);
            Authentication auth = mockAuthenticationWith(details);

            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(jwtService.generateToken(any())).thenReturn("token");
            when(jwtService.getJwtExpirationMs()).thenReturn(3600000L);

            authService.login(buildLoginRequest("  ALICE@EXAMPLE.COM  ", "Secret1!"));

            ArgumentCaptor<org.springframework.security.authentication.UsernamePasswordAuthenticationToken> captor =
                    ArgumentCaptor.forClass(
                            org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class
                    );
            verify(authenticationManager).authenticate(captor.capture());
            assertThat(captor.getValue().getPrincipal()).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("throws BadRequestException when dto is null")
        void throwsForNullDto() {
            assertThatThrownBy(() -> authService.login(null))
                    .isInstanceOf(BadRequestException.class);
            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("throws BadRequestException when email is null")
        void throwsForNullEmail() {
            assertThatThrownBy(() -> authService.login(buildLoginRequest(null, "Secret1!")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Email");
        }

        @ParameterizedTest(name = "email=\"{0}\"")
        @ValueSource(strings = {"", "   "})
        @DisplayName("throws BadRequestException when email is blank")
        void throwsForBlankEmail(String blank) {
            assertThatThrownBy(() -> authService.login(buildLoginRequest(blank, "Secret1!")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Email");
        }

        @Test
        @DisplayName("throws BadRequestException when credentials are wrong (BadCredentialsException)")
        void throwsForBadCredentials() {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("bad credentials"));

            assertThatThrownBy(() ->
                    authService.login(buildLoginRequest("alice@example.com", "wrongpass")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        @DisplayName("throws ForbiddenOperationException when Spring Security throws DisabledException")
        void throwsForDisabledViaSpring() {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new DisabledException("account disabled"));

            assertThatThrownBy(() ->
                    authService.login(buildLoginRequest("alice@example.com", "Secret1!")))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("disabled");
        }

        @Test
        @DisplayName("EDGE: throws BadRequestException when principal is not CustomUserDetails")
        void throwsWhenPrincipalIsNotCustomUserDetails() {
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn("unexpected-string-principal");
            when(authenticationManager.authenticate(any())).thenReturn(auth);

            assertThatThrownBy(() ->
                    authService.login(buildLoginRequest("alice@example.com", "Secret1!")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Authentication failed");
        }

        @Test
        @DisplayName("EDGE: throws BadRequestException when authenticated user no longer exists in DB")
        void throwsWhenUserNotFoundAfterAuth() {
            User user = buildSavedUser(99L, "ghost@example.com", "ghost", Role.USER);
            CustomUserDetails details = new CustomUserDetails(user);
            Authentication auth = mockAuthenticationWith(details);

            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    authService.login(buildLoginRequest("ghost@example.com", "Secret1!")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("EDGE: throws ForbiddenOperationException when user is disabled at DB level after auth")
        void throwsWhenUserDisabledAtDbLevel() {
            User user = buildSavedUser(1L, "alice@example.com", "alice99", Role.USER);
            user.setEnabled(false);
            CustomUserDetails details = new CustomUserDetails(user);
            Authentication auth = mockAuthenticationWith(details);

            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertThatThrownBy(() ->
                    authService.login(buildLoginRequest("alice@example.com", "Secret1!")))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("disabled");
        }
    }
}
