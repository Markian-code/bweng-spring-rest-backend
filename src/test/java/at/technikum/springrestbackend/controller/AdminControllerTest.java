package at.technikum.springrestbackend.controller;

import at.technikum.springrestbackend.dto.BookResponseDto;
import at.technikum.springrestbackend.dto.CommentResponseDto;
import at.technikum.springrestbackend.dto.UserResponseDto;
import at.technikum.springrestbackend.entity.Role;
import at.technikum.springrestbackend.entity.User;
import at.technikum.springrestbackend.exception.BadRequestException;
import at.technikum.springrestbackend.security.CustomUserDetails;
import at.technikum.springrestbackend.service.BookService;
import at.technikum.springrestbackend.service.CommentService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController")
class AdminControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private BookService bookService;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private AdminController controller;

    private User buildUser(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@test.com");
        user.setUsername("user_" + id);
        user.setPasswordHash("hash");
        user.setCountryCode("AT");
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private CustomUserDetails buildPrincipal(Long id, Role role) {
        return new CustomUserDetails(buildUser(id, role));
    }

    @Test
    @DisplayName("any endpoint: throws BadRequestException when principal is null")
    void throwsForNullPrincipal() {
        assertThatThrownBy(() -> controller.getAllUsers(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("required");
    }

    //  getAllUsers

    @Nested
    @DisplayName("GET /admin/users")
    class GetAllUsers {

        @Test
        @DisplayName("returns 200 with user list for admin")
        void returns200WithUserList() {
            CustomUserDetails principal = buildPrincipal(1L, Role.ADMIN);
            User adminEntity = buildUser(1L, Role.ADMIN);
            when(userService.getUserEntityById(1L)).thenReturn(adminEntity);
            when(userService.getAllUsersForAdmin(adminEntity)).thenReturn(List.of(new UserResponseDto()));

            ResponseEntity<List<UserResponseDto>> result = controller.getAllUsers(principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).hasSize(1);
        }
    }

   //  getUserById

    @Nested
    @DisplayName("GET /admin/users/{userId}")
    class GetUserById {

        @Test
        @DisplayName("returns 200 with specific user details for admin")
        void returns200WithUser() {
            CustomUserDetails principal = buildPrincipal(1L, Role.ADMIN);
            User adminEntity = buildUser(1L, Role.ADMIN);
            UserResponseDto dto = new UserResponseDto();
            when(userService.getUserEntityById(1L)).thenReturn(adminEntity);
            when(userService.getUserByIdForAdmin(99L, adminEntity)).thenReturn(dto);

            ResponseEntity<UserResponseDto> result = controller.getUserById(99L, principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

   //  setUserEnabled

    @Nested
    @DisplayName("PATCH /admin/users/{userId}/enabled")
    class SetUserEnabled {

        @Test
        @DisplayName("returns 200 with updated user after enabling")
        void returns200AfterEnable() {
            CustomUserDetails principal = buildPrincipal(1L, Role.ADMIN);
            User adminEntity = buildUser(1L, Role.ADMIN);
            UserResponseDto dto = new UserResponseDto();
            when(userService.getUserEntityById(1L)).thenReturn(adminEntity);
            when(userService.setUserEnabled(5L, true, adminEntity)).thenReturn(dto);

            ResponseEntity<UserResponseDto> result = controller.setUserEnabled(5L, true, principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    //  toggleUserEnabled

    @Nested
    @DisplayName("PATCH /admin/users/{userId}/toggle-enabled")
    class ToggleUserEnabled {

        @Test
        @DisplayName("returns 200 with updated user after toggle")
        void returns200AfterToggle() {
            CustomUserDetails principal = buildPrincipal(1L, Role.ADMIN);
            User adminEntity = buildUser(1L, Role.ADMIN);
            UserResponseDto dto = new UserResponseDto();
            when(userService.getUserEntityById(1L)).thenReturn(adminEntity);
            when(userService.toggleUserEnabled(7L, adminEntity)).thenReturn(dto);

            ResponseEntity<UserResponseDto> result = controller.toggleUserEnabled(7L, principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    //  getAllBooks

    @Nested
    @DisplayName("GET /admin/books")
    class GetAllBooks {

        @Test
        @DisplayName("returns 200 with all books for admin")
        void returns200WithBooks() {
            CustomUserDetails principal = buildPrincipal(1L, Role.ADMIN);
            User adminEntity = buildUser(1L, Role.ADMIN);
            when(userService.getUserEntityById(1L)).thenReturn(adminEntity);
            when(bookService.getAllBooksForAdmin(adminEntity)).thenReturn(List.of(new BookResponseDto()));

            ResponseEntity<List<BookResponseDto>> result = controller.getAllBooks(principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).hasSize(1);
        }
    }

   //  getAllComments

    @Nested
    @DisplayName("GET /admin/comments")
    class GetAllComments {

        @Test
        @DisplayName("returns 200 with all comments for admin")
        void returns200WithComments() {
            CustomUserDetails principal = buildPrincipal(1L, Role.ADMIN);
            User adminEntity = buildUser(1L, Role.ADMIN);
            when(userService.getUserEntityById(1L)).thenReturn(adminEntity);
            when(commentService.getAllCommentsForAdmin(adminEntity)).thenReturn(List.of());

            ResponseEntity<List<CommentResponseDto>> result = controller.getAllComments(principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
