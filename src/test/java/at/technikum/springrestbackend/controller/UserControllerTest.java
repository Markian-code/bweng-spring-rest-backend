package at.technikum.springrestbackend.controller;

import at.technikum.springrestbackend.dto.UserResponseDto;
import at.technikum.springrestbackend.dto.UserUpdateRequestDto;
import at.technikum.springrestbackend.entity.Role;
import at.technikum.springrestbackend.entity.User;
import at.technikum.springrestbackend.exception.BadRequestException;
import at.technikum.springrestbackend.security.CustomUserDetails;
import at.technikum.springrestbackend.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController controller;

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@test.com");
        user.setUsername("user_" + id);
        user.setPasswordHash("hash");
        user.setCountryCode("AT");
        user.setRole(Role.USER);
        user.setEnabled(true);
        return user;
    }

    private CustomUserDetails buildPrincipal(Long id) {
        return new CustomUserDetails(buildUser(id));
    }

    //  getCurrentUserProfile

    @Nested
    @DisplayName("GET /users/me")
    class GetCurrentUserProfile {

        @Test
        @DisplayName("returns 200 with user profile for authenticated user")
        void returns200ForAuthenticatedUser() {
            CustomUserDetails principal = buildPrincipal(1L);
            User userEntity = buildUser(1L);
            UserResponseDto dto = new UserResponseDto();
            dto.setId(1L);
            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(userService.getCurrentUserProfile(userEntity)).thenReturn(dto);

            ResponseEntity<UserResponseDto> result = controller.getCurrentUserProfile(principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("throws BadRequestException when principal is null")
        void throwsForNullPrincipal() {
            assertThatThrownBy(() -> controller.getCurrentUserProfile(null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("required");
        }
    }

    //  updateCurrentUserProfile

    @Nested
    @DisplayName("PUT /users/me")
    class UpdateCurrentUserProfile {

        @Test
        @DisplayName("returns 200 with updated profile for authenticated user")
        void returns200ForAuthenticatedUser() {
            CustomUserDetails principal = buildPrincipal(2L);
            User userEntity = buildUser(2L);
            UserUpdateRequestDto request = new UserUpdateRequestDto();
            UserResponseDto dto = new UserResponseDto();
            dto.setId(2L);
            when(userService.getUserEntityById(2L)).thenReturn(userEntity);
            when(userService.updateCurrentUserProfile(request, userEntity)).thenReturn(dto);

            ResponseEntity<UserResponseDto> result = controller.updateCurrentUserProfile(request, principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("throws BadRequestException when principal is null")
        void throwsForNullPrincipal() {
            assertThatThrownBy(() ->
                    controller.updateCurrentUserProfile(new UserUpdateRequestDto(), null))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
