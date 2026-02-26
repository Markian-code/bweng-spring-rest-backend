package at.technikum.springrestbackend.controller;

import at.technikum.springrestbackend.dto.UserResponseDto;
import at.technikum.springrestbackend.dto.UserUpdateRequestDto;
import at.technikum.springrestbackend.entity.User;
import at.technikum.springrestbackend.exception.BadRequestException;
import at.technikum.springrestbackend.security.CustomUserDetails;
import at.technikum.springrestbackend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUserProfile(
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        UserResponseDto response = userService.getCurrentUserProfile(currentUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDto> updateCurrentUserProfile(
            @Valid @RequestBody final UserUpdateRequestDto request,
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        UserResponseDto response = userService.updateCurrentUserProfile(request, currentUser);
        return ResponseEntity.ok(response);
    }

    private User resolveCurrentUser(final CustomUserDetails principal) {
        if (principal == null || principal.getId() == null) {
            throw new BadRequestException("Authenticated user is required");
        }

        return userService.getUserEntityById(principal.getId());
    }
}