package at.technikum.springrestbackend.controller;

import at.technikum.springrestbackend.dto.AuthRequestDto;
import at.technikum.springrestbackend.dto.AuthResponseDto;
import at.technikum.springrestbackend.dto.RegisterRequestDto;
import at.technikum.springrestbackend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(final AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(
            @Valid @RequestBody final RegisterRequestDto request
    ) {
        AuthResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody final AuthRequestDto request
    ) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}