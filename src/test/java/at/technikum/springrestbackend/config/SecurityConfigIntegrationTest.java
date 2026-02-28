package at.technikum.springrestbackend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SecurityConfig (integration — filter chain)")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /admin/** returns 401 for unauthenticated requests")
    void adminEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /books is publicly accessible without authentication")
    void publicBooksEndpointIsPermitted() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /books/me requires authentication")
    void booksMeEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/books/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /comments/me requires authentication")
    void commentsMeEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/comments/me"))
                .andExpect(status().isUnauthorized());
    }
}