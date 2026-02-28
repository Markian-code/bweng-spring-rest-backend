package at.technikum.springrestbackend.controller;

import at.technikum.springrestbackend.dto.AuthRequestDto;
import at.technikum.springrestbackend.dto.AuthResponseDto;
import at.technikum.springrestbackend.dto.RegisterRequestDto;
import at.technikum.springrestbackend.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    private AuthResponseDto buildAuthResponse() {
        AuthResponseDto dto = new AuthResponseDto();
        dto.setAccessToken("jwt-token");
        dto.setEmail("user@test.com");
        return dto;
    }

    @Test
    @DisplayName("register: returns 201 CREATED with auth response")
    void register_returns201() {
        RegisterRequestDto request = new RegisterRequestDto();
        AuthResponseDto authResponse = buildAuthResponse();
        when(authService.register(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponseDto> result = controller.register(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(authResponse);
        assertThat(result.getBody().getAccessToken()).isEqualTo("jwt-token");
    }

    @Test
    @DisplayName("login: returns 200 OK with auth response")
    void login_returns200() {
        AuthRequestDto request = new AuthRequestDto();
        AuthResponseDto authResponse = buildAuthResponse();
        when(authService.login(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponseDto> result = controller.login(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(authResponse);
    }
}
