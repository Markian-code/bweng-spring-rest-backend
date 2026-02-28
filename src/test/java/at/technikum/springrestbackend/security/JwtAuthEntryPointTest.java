package at.technikum.springrestbackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthEntryPoint")
class JwtAuthEntryPointTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @InjectMocks
    private JwtAuthEntryPoint entryPoint;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() throws Exception {
        responseBody = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new jakarta.servlet.ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
            }

            @Override
            public void write(int b) {
                responseBody.write(b);
            }
        });
    }

    @Test
    @DisplayName("sets 401 status and JSON content-type on response")
    void setsStatusAndContentType() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/books");
        AuthenticationException ex = mock(AuthenticationException.class);

        entryPoint.commence(request, response, ex);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    @DisplayName("writes JSON body containing status 401, error and fixed message")
    void writesJsonBody() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/secure");
        AuthenticationException ex = mock(AuthenticationException.class);

        entryPoint.commence(request, response, ex);

        String json = responseBody.toString();
        assertThat(json).contains("\"status\":401");
        assertThat(json).contains("\"error\":\"Unauthorized\"");
        assertThat(json).contains("Authentication is required");
        assertThat(json).contains("/api/secure");
    }
}
