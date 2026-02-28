package at.technikum.springrestbackend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    private HttpServletRequest mockRequest(String uri) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn(uri);
        return req;
    }

     //  handleNotFound  →  404

    @Nested
    @DisplayName("handleNotFound → 404 Not Found")
    class HandleNotFound {

        @Test
        @DisplayName("returns 404 with exception message and request path")
        void returns404WithMessage() {
            HttpServletRequest req = mockRequest("/api/books/99");
            ResourceNotFoundException ex = new ResourceNotFoundException("Book not found with id: 99");

            ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(ex, req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            ApiErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getStatus()).isEqualTo(404);
            assertThat(body.getError()).isEqualTo("Not Found");
            assertThat(body.getMessage()).isEqualTo("Book not found with id: 99");
            assertThat(body.getPath()).isEqualTo("/api/books/99");
            assertThat(body.getTimestamp()).isNotNull();
        }
    }

    //  handleBadRequest  →  400

    @Nested
    @DisplayName("handleBadRequest → 400 Bad Request")
    class HandleBadRequest {

        @Test
        @DisplayName("returns 400 for BadRequestException with its message")
        void returns400ForBadRequestException() {
            HttpServletRequest req = mockRequest("/api/users");
            BadRequestException ex = new BadRequestException("Username already taken");

            ResponseEntity<ApiErrorResponse> response = handler.handleBadRequest(ex, req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).isEqualTo("Username already taken");
            assertThat(response.getBody().getPath()).isEqualTo("/api/users");
        }

        @Test
        @DisplayName("returns 400 for InvalidFileTypeException")
        void returns400ForInvalidFileTypeException() {
            HttpServletRequest req = mockRequest("/api/books/1/image");
            InvalidFileTypeException ex = new InvalidFileTypeException("Only JPEG/PNG allowed");

            ResponseEntity<ApiErrorResponse> response = handler.handleBadRequest(ex, req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getMessage()).isEqualTo("Only JPEG/PNG allowed");
        }
    }

     //  handleConflict  →  409

    @Nested
    @DisplayName("handleConflict → 409 Conflict")
    class HandleConflict {

        @Test
        @DisplayName("returns 409 with the exception message")
        void returns409WithMessage() {
            HttpServletRequest req = mockRequest("/api/auth/register");
            ConflictException ex = new ConflictException("Email is already in use");

            ResponseEntity<ApiErrorResponse> response = handler.handleConflict(ex, req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().getStatus()).isEqualTo(409);
            assertThat(response.getBody().getError()).isEqualTo("Conflict");
            assertThat(response.getBody().getMessage()).isEqualTo("Email is already in use");
            assertThat(response.getBody().getPath()).isEqualTo("/api/auth/register");
        }
    }

   //  handleForbiddenOperation  →  403

    @Nested
    @DisplayName("handleForbiddenOperation → 403 Forbidden")
    class HandleForbiddenOperation {

        @Test
        @DisplayName("returns 403 with the exception message")
        void returns403WithMessage() {
            HttpServletRequest req = mockRequest("/api/admin/users");
            ForbiddenOperationException ex = new ForbiddenOperationException("Admin privileges required");

            ResponseEntity<ApiErrorResponse> response = handler.handleForbiddenOperation(ex, req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody().getStatus()).isEqualTo(403);
            assertThat(response.getBody().getMessage()).isEqualTo("Admin privileges required");
            assertThat(response.getBody().getError()).isEqualTo("Forbidden");
        }
    }

   //  handleAccessDenied  →  403 (fixed message)

    @Nested
    @DisplayName("handleAccessDenied → 403 Forbidden (fixed message)")
    class HandleAccessDenied {

        @Test
        @DisplayName("returns 403 with fixed 'Access denied' message regardless of exception message")
        void returns403WithFixedMessage() {
            HttpServletRequest req = mockRequest("/api/books");
            AccessDeniedException ex = new AccessDeniedException("Spring security denied");

            ResponseEntity<ApiErrorResponse> response = handler.handleAccessDenied(ex, req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
        }
    }

   //  handleMaxUploadSizeExceeded  →  400 (fixed message)

    @Nested
    @DisplayName("handleMaxUploadSizeExceeded → 400 Bad Request (fixed message)")
    class HandleMaxUploadSizeExceeded {

        @Test
        @DisplayName("returns 400 with fixed 'Uploaded file is too large' message")
        void returns400WithFixedMessage() {
            HttpServletRequest req = mockRequest("/api/books/1/image");
            MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(1024L);

            ResponseEntity<ApiErrorResponse> response = handler.handleMaxUploadSizeExceeded(ex, req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getMessage()).isEqualTo("Uploaded file is too large");
        }
    }

   //  handleMethodArgumentNotValid  →  400 with field-error details

    @Nested
    @DisplayName("handleMethodArgumentNotValid → 400 with validation details")
    class HandleMethodArgumentNotValid {

        @Test
        @DisplayName("returns 400 with 'Validation failed' message and field error details")
        void returns400WithFieldErrors() {
            HttpServletRequest req = mockRequest("/api/auth/register");

            FieldError fieldError1 = new FieldError("req", "email", "must not be blank");
            FieldError fieldError2 = new FieldError("req", "username", "size must be between 3 and 50");

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<ApiErrorResponse> response = handler.handleMethodArgumentNotValid(ex, req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ApiErrorResponse body = response.getBody();
            assertThat(body.getMessage()).isEqualTo("Validation failed");
            assertThat(body.getDetails()).containsExactlyInAnyOrder(
                    "email: must not be blank",
                    "username: size must be between 3 and 50"
            );
        }

        @Test
        @DisplayName("returns empty details list when there are no field errors")
        void returnsEmptyDetailsWhenNoFieldErrors() {
            HttpServletRequest req = mockRequest("/api/auth/register");

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of());

            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<ApiErrorResponse> response = handler.handleMethodArgumentNotValid(ex, req);

            assertThat(response.getBody().getDetails()).isEmpty();
        }
    }

   //  handleConstraintViolation  →  400 with violation details

    @Nested
    @DisplayName("handleConstraintViolation → 400 with violation details")
    class HandleConstraintViolation {

        @Test
        @DisplayName("returns 400 with 'Validation failed' and constraint violation details")
        void returns400WithViolationDetails() {
            HttpServletRequest req = mockRequest("/api/books");

            Path path1 = mock(Path.class);
            when(path1.toString()).thenReturn("title");
            ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
            when(violation1.getPropertyPath()).thenReturn(path1);
            when(violation1.getMessage()).thenReturn("must not be blank");

            ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation1));

            ResponseEntity<ApiErrorResponse> response = handler.handleConstraintViolation(ex, req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
            assertThat(response.getBody().getDetails()).containsExactly("title: must not be blank");
        }
    }

   //  handleGenericException  →  500 (fixed message, hides internal details)

    @Nested
    @DisplayName("handleGenericException → 500 Internal Server Error")
    class HandleGenericException {

        @Test
        @DisplayName("returns 500 with fixed 'Unexpected server error' regardless of cause")
        void returns500WithFixedMessage() {
            HttpServletRequest req = mockRequest("/api/books");
            Exception ex = new RuntimeException("NullPointerException in service layer");

            ResponseEntity<ApiErrorResponse> response = handler.handleGenericException(ex, req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getMessage()).isEqualTo("Unexpected server error");
            assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        }
    }
}