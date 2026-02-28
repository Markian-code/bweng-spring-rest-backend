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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String DEFAULT_PROFILE_PICTURE_PLACEHOLDER =
            "/images/profile-placeholder.png";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            final UserRepository userRepository,
            final PasswordEncoder passwordEncoder,
            final AuthenticationManager authenticationManager,
            final JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponseDto register(final RegisterRequestDto dto) {
        if (dto == null) {
            throw new BadRequestException("Register request must not be null");
        }
        String normalizedEmail = normalizeEmail(dto.getEmail());
        String normalizedUsername = normalizeUsername(dto.getUsername());
        String normalizedCountryCode = normalizeCountryCode(dto.getCountryCode());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email is already in use");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BadRequestException("Username is already in use");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setCountryCode(normalizedCountryCode);
        user.setProfilePictureUrl(DEFAULT_PROFILE_PICTURE_PLACEHOLDER);
        user.setRole(Role.USER);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);
        return buildAuthResponse(savedUser, token);
    }

    public AuthResponseDto login(final AuthRequestDto dto) {
        if (dto == null) {
            throw new BadRequestException("Login request must not be null");
        }
        String normalizedEmail = normalizeEmail(dto.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, dto.getPassword()));

            if (!(authentication.getPrincipal() instanceof CustomUserDetails customUserDetails)) {
                throw new BadRequestException("Authentication failed");
            }
            User user = userRepository.findById(customUserDetails.getId())
                    .orElseThrow(() -> new BadRequestException("Authenticated user not found"));

            if (!user.isEnabled()) {
                throw new ForbiddenOperationException("User account is disabled");
            }
            String token = jwtService.generateToken(user);
            return buildAuthResponse(user, token);

        } catch (BadCredentialsException ex) {
            throw new BadRequestException("Invalid email or password");
        } catch (DisabledException ex) {
            throw new ForbiddenOperationException("User account is disabled");
        }
    }

    private AuthResponseDto buildAuthResponse(final User user, final String token) {
        AuthResponseDto response = new AuthResponseDto();
        response.setAccessToken(token);
        response.setTokenType("Bearer");
        response.setExpiresInMs(jwtService.getJwtExpirationMs());
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        return response;
    }

    private String normalizeEmail(final String email) {
        if (email == null) {
            throw new BadRequestException("Email is required");
        }
        String normalized = email.trim().toLowerCase();
        if (normalized.isBlank()) {
            throw new BadRequestException("Email must not be blank");
        }
        return normalized;
    }

    private String normalizeUsername(final String username) {
        if (username == null) {
            throw new BadRequestException("Username is required");
        }
        String normalized = username.trim();
        if (normalized.isBlank()) {
            throw new BadRequestException("Username must not be blank");
        }
        return normalized;
    }

    private String normalizeCountryCode(final String countryCode) {
        if (countryCode == null) {
            throw new BadRequestException("Country code is required");
        }
        String normalized = countryCode.trim().toUpperCase();
        if (normalized.isBlank()) {
            throw new BadRequestException("Country code must not be blank");
        }
        return normalized;
    }
}