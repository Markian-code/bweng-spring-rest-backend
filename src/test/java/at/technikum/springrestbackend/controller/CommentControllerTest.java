package at.technikum.springrestbackend.controller;

import at.technikum.springrestbackend.dto.CommentCreateRequestDto;
import at.technikum.springrestbackend.dto.CommentResponseDto;
import at.technikum.springrestbackend.dto.CommentUpdateRequestDto;
import at.technikum.springrestbackend.entity.Role;
import at.technikum.springrestbackend.entity.User;
import at.technikum.springrestbackend.exception.BadRequestException;
import at.technikum.springrestbackend.security.CustomUserDetails;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentController")
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @Mock
    private UserService userService;

    @InjectMocks
    private CommentController controller;

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

    //  getCommentsForBook

    @Nested
    @DisplayName("GET /comments/book/{bookId}")
    class GetCommentsForBook {

        @Test
        @DisplayName("returns 200 with comment list for book")
        void returns200WithComments() {
            CommentResponseDto dto = new CommentResponseDto();
            when(commentService.getCommentsForPublicBook(10L)).thenReturn(List.of(dto));

            ResponseEntity<List<CommentResponseDto>> result = controller.getCommentsForBook(10L);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).hasSize(1);
        }
    }

    //  getMyComments

    @Nested
    @DisplayName("GET /comments/me")
    class GetMyComments {

        @Test
        @DisplayName("returns 200 with current user's comments")
        void returns200ForAuthenticatedUser() {
            CustomUserDetails principal = buildPrincipal(1L);
            User userEntity = buildUser(1L);
            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(commentService.getCommentsOfUser(userEntity)).thenReturn(List.of());

            ResponseEntity<List<CommentResponseDto>> result = controller.getMyComments(principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("throws BadRequestException when principal is null")
        void throwsForNullPrincipal() {
            assertThatThrownBy(() -> controller.getMyComments(null))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    //  createComment

    @Nested
    @DisplayName("POST /comments/book/{bookId}")
    class CreateComment {

        @Test
        @DisplayName("returns 201 CREATED with created comment")
        void returns201ForAuthenticatedUser() {
            CustomUserDetails principal = buildPrincipal(1L);
            User userEntity = buildUser(1L);
            CommentCreateRequestDto request = new CommentCreateRequestDto();
            CommentResponseDto dto = new CommentResponseDto();
            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(commentService.createComment(request, userEntity)).thenReturn(dto);

            ResponseEntity<CommentResponseDto> result = controller.createComment(5L, request, principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(request.getBookId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("throws BadRequestException when principal is null")
        void throwsForNullPrincipal() {
            assertThatThrownBy(() ->
                    controller.createComment(5L, new CommentCreateRequestDto(), null))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    //  updateComment

    @Nested
    @DisplayName("PUT /comments/{commentId}")
    class UpdateComment {

        @Test
        @DisplayName("returns 200 OK with updated comment")
        void returns200ForAuthenticatedUser() {
            CustomUserDetails principal = buildPrincipal(1L);
            User userEntity = buildUser(1L);
            CommentUpdateRequestDto request = new CommentUpdateRequestDto();
            CommentResponseDto dto = new CommentResponseDto();
            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(commentService.updateComment(3L, request, userEntity)).thenReturn(dto);

            ResponseEntity<CommentResponseDto> result = controller.updateComment(3L, request, principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    //  deleteComment

    @Nested
    @DisplayName("DELETE /comments/{commentId}")
    class DeleteComment {

        @Test
        @DisplayName("returns 204 NO CONTENT after deletion")
        void returns204ForAuthenticatedUser() {
            CustomUserDetails principal = buildPrincipal(1L);
            User userEntity = buildUser(1L);
            when(userService.getUserEntityById(1L)).thenReturn(userEntity);

            ResponseEntity<Void> result = controller.deleteComment(7L, principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(commentService).deleteComment(7L, userEntity);
        }
    }
}
