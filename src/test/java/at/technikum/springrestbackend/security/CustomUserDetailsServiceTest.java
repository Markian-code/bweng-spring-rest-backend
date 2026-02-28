package at.technikum.springrestbackend.security;

import at.technikum.springrestbackend.entity.Role;
import at.technikum.springrestbackend.entity.User;
import at.technikum.springrestbackend.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername("user_" + id);
        user.setPasswordHash("hash");
        user.setCountryCode("AT");
        user.setRole(Role.USER);
        user.setEnabled(true);
        return user;
    }

    //  loadUserByUsername

    @Nested
    @DisplayName("loadUserByUsername(String)")
    class LoadUserByUsername {

        @Test
        @DisplayName("returns CustomUserDetails when user exists with given email")
        void returnsUserDetailsForExistingEmail() {
            User user = buildUser(1L, "alice@test.com");
            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

            UserDetails details = service.loadUserByUsername("alice@test.com");

            assertThat(details).isInstanceOf(CustomUserDetails.class);
            assertThat(((CustomUserDetails) details).getId()).isEqualTo(1L);
            assertThat(details.getUsername()).isEqualTo("alice@test.com");
        }

        @Test
        @DisplayName("throws UsernameNotFoundException when email is not found")
        void throwsWhenEmailNotFound() {
            when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadUserByUsername("missing@test.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("missing@test.com");
        }
    }

     //  loadUserById

    @Nested
    @DisplayName("loadUserById(Long)")
    class LoadUserById {

        @Test
        @DisplayName("returns CustomUserDetails when user exists with given ID")
        void returnsUserDetailsForExistingId() {
            User user = buildUser(42L, "bob@test.com");
            when(userRepository.findById(42L)).thenReturn(Optional.of(user));

            UserDetails details = service.loadUserById(42L);

            assertThat(details).isInstanceOf(CustomUserDetails.class);
            assertThat(((CustomUserDetails) details).getId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("throws UsernameNotFoundException when user ID is not found")
        void throwsWhenIdNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadUserById(999L))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }
}