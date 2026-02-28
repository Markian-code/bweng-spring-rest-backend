package at.technikum.springrestbackend.service;

import at.technikum.springrestbackend.dto.UserResponseDto;
import at.technikum.springrestbackend.dto.UserUpdateRequestDto;
import at.technikum.springrestbackend.entity.Role;
import at.technikum.springrestbackend.entity.User;
import at.technikum.springrestbackend.exception.BadRequestException;
import at.technikum.springrestbackend.exception.ForbiddenOperationException;
import at.technikum.springrestbackend.exception.ResourceNotFoundException;
import at.technikum.springrestbackend.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User buildUser(Long id, String username, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(username + "@test.com");
        user.setUsername(username);
        user.setPasswordHash("$2a$hash");
        user.setCountryCode("AT");
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private UserUpdateRequestDto buildUpdateRequest(
            String username, String countryCode, String profilePictureUrl
    ) {
        UserUpdateRequestDto dto = new UserUpdateRequestDto();
        dto.setUsername(username);
        dto.setCountryCode(countryCode);
        dto.setProfilePictureUrl(profilePictureUrl);
        return dto;
    }

    @Nested
    @DisplayName("getUserEntityById(Long)")
    class GetUserEntityById {

        @Test
        @DisplayName("returns User entity when found")
        void returnsUserEntity() {
            User user = buildUser(1L, "alice", Role.USER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            User result = userService.getUserEntityById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("alice");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user does not exist")
        void throwsWhenNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserEntityById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("getCurrentUserProfile(User)")
    class GetCurrentUserProfile {

        @Test
        @DisplayName("loads fresh user from DB and maps all fields to DTO")
        void returnsFreshProfileFromDb() {
            User storedUser = buildUser(5L, "bob", Role.USER);
            storedUser.setCountryCode("DE");
            storedUser.setProfilePictureUrl("http://img.com/bob.jpg");
            when(userRepository.findById(5L)).thenReturn(Optional.of(storedUser));

            User principal = buildUser(5L, "bob", Role.USER);
            UserResponseDto result = userService.getCurrentUserProfile(principal);

            assertThat(result.getId()).isEqualTo(5L);
            assertThat(result.getUsername()).isEqualTo("bob");
            assertThat(result.getEmail()).isEqualTo("bob@test.com");
            assertThat(result.getCountryCode()).isEqualTo("DE");
            assertThat(result.getProfilePictureUrl()).isEqualTo("http://img.com/bob.jpg");
            assertThat(result.getRole()).isEqualTo(Role.USER);
            assertThat(result.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("updateCurrentUserProfile(UserUpdateRequestDto, User)")
    class UpdateCurrentUserProfile {

        @Test
        @DisplayName("updates username, countryCode and profilePictureUrl when all provided")
        void updatesAllFields() {
            User stored = buildUser(1L, "alice", Role.USER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(stored));
            when(userRepository.existsByUsername("newAlice")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserUpdateRequestDto request = buildUpdateRequest(
                    "newAlice", "de", "http://img.com/new.jpg"
            );
            UserResponseDto result = userService.updateCurrentUserProfile(request, stored);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getUsername()).isEqualTo("newAlice");
            assertThat(captor.getValue().getCountryCode()).isEqualTo("DE");
            assertThat(captor.getValue().getProfilePictureUrl()).isEqualTo("http://img.com/new.jpg");
            assertThat(result.getUsername()).isEqualTo("newAlice");
        }

        @Test
        @DisplayName("countryCode is uppercased before saving")
        void countryCodeIsUppercased() {
            User stored = buildUser(1L, "alice", Role.USER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(stored));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            userService.updateCurrentUserProfile(
                    buildUpdateRequest(null, "at", null), stored
            );

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getCountryCode()).isEqualTo("AT");
        }

        @Test
        @DisplayName("skips username update when username is null in request")
        void skipsUsernameWhenNull() {
            User stored = buildUser(1L, "alice", Role.USER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(stored));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            userService.updateCurrentUserProfile(
                    buildUpdateRequest(null, null, null), stored
            );

            verify(userRepository, never()).existsByUsername(anyString());
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getUsername()).isEqualTo("alice");
        }

        @Test
        @DisplayName("skips username update when username is blank")
        void skipsUsernameWhenBlank() {
            User stored = buildUser(1L, "alice", Role.USER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(stored));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            userService.updateCurrentUserProfile(
                    buildUpdateRequest("   ", null, null), stored
            );

            verify(userRepository, never()).existsByUsername(anyString());
        }

        @Test
        @DisplayName("EDGE: username with surrounding spaces is trimmed before save and uniqueness check")
        void usernameWithSpacesIsTrimmed() {
            User stored = buildUser(1L, "alice", Role.USER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(stored));
            when(userRepository.existsByUsername("alice2")).thenReturn(false);
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            userService.updateCurrentUserProfile(
                    buildUpdateRequest("  alice2  ", null, null), stored
            );

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getUsername()).isEqualTo("alice2");
            verify(userRepository).existsByUsername("alice2");
        }

        @Test
        @DisplayName("skips countryCode update when countryCode is blank")
        void skipsCountryCodeWhenBlank() {
            User stored = buildUser(1L, "alice", Role.USER);
            stored.setCountryCode("AT");
            when(userRepository.findById(1L)).thenReturn(Optional.of(stored));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            userService.updateCurrentUserProfile(
                    buildUpdateRequest(null, "   ", null), stored
            );

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getCountryCode()).isEqualTo("AT");
        }

        @Test
        @DisplayName("EDGE: profilePictureUrl with surrounding spaces is trimmed before saving")
        void profilePictureUrlWithSpacesIsTrimmed() {
            User stored = buildUser(1L, "alice", Role.USER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(stored));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            userService.updateCurrentUserProfile(
                    buildUpdateRequest(null, null, "  http://img.com/pic.jpg  "), stored
            );

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getProfilePictureUrl()).isEqualTo("http://img.com/pic.jpg");
        }

        @Test
        @DisplayName("EDGE: case-only username change skips uniqueness check and applies update")
        void caseOnlyUsernameChangeSkipsUniquenessCheck() {
            User stored = buildUser(1L, "Alice", Role.USER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(stored));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            userService.updateCurrentUserProfile(
                    buildUpdateRequest("alice", null, null), stored
            );

            verify(userRepository, never()).existsByUsername(anyString());

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getUsername()).isEqualTo("alice");
        }

        @Test
        @DisplayName("throws BadRequestException when new username is already taken")
        void throwsWhenUsernameTaken() {
            User stored = buildUser(1L, "alice", Role.USER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(stored));
            when(userRepository.existsByUsername("bob")).thenReturn(true);

            assertThatThrownBy(() ->
                    userService.updateCurrentUserProfile(
                            buildUpdateRequest("bob", null, null), stored
                    ))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("taken");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("EDGE: null profilePictureUrl skips update — existing picture preserved")
        void nullProfilePictureUrlIsSkipped() {
            User stored = buildUser(1L, "alice", Role.USER);
            stored.setProfilePictureUrl("http://existing.com/pic.jpg");
            when(userRepository.findById(1L)).thenReturn(Optional.of(stored));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            userService.updateCurrentUserProfile(
                    buildUpdateRequest(null, null, null), stored
            );

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getProfilePictureUrl())
                    .isEqualTo("http://existing.com/pic.jpg");
        }

        @Test
        @DisplayName("EDGE: blank profilePictureUrl clears the picture (sets to null)")
        void blankProfilePictureUrlClearsPicture() {
            User stored = buildUser(1L, "alice", Role.USER);
            stored.setProfilePictureUrl("http://existing.com/pic.jpg");
            when(userRepository.findById(1L)).thenReturn(Optional.of(stored));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            userService.updateCurrentUserProfile(
                    buildUpdateRequest(null, null, "   "), stored
            );

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getProfilePictureUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("getAllUsersForAdmin(User)")
    class GetAllUsersForAdmin {

        @Test
        @DisplayName("returns all users mapped to DTOs when caller is ADMIN")
        void returnsAllUsersForAdmin() {
            User admin = buildUser(1L, "admin", Role.ADMIN);
            List<User> allUsers = List.of(
                    buildUser(1L, "admin", Role.ADMIN),
                    buildUser(2L, "bob", Role.USER),
                    buildUser(3L, "carol", Role.USER)
            );
            when(userRepository.findAll()).thenReturn(allUsers);

            List<UserResponseDto> result = userService.getAllUsersForAdmin(admin);

            assertThat(result).hasSize(3);
            assertThat(result.get(1).getUsername()).isEqualTo("bob");
        }

        @Test
        @DisplayName("throws ForbiddenOperationException for non-admin user")
        void throwsForNonAdmin() {
            User regularUser = buildUser(1L, "bob", Role.USER);

            assertThatThrownBy(() -> userService.getAllUsersForAdmin(regularUser))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("Admin");
            verify(userRepository, never()).findAll();
        }

        @Test
        @DisplayName("throws ForbiddenOperationException when user is null (requireAdmin guard)")
        void throwsWhenUserIsNull() {
            assertThatThrownBy(() -> userService.getAllUsersForAdmin(null))
                    .isInstanceOf(ForbiddenOperationException.class);
            verify(userRepository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("getUserByIdForAdmin(Long, User)")
    class GetUserByIdForAdmin {

        @Test
        @DisplayName("admin can look up any user by id")
        void adminCanGetAnyUser() {
            User admin = buildUser(1L, "admin", Role.ADMIN);
            User target = buildUser(42L, "carol", Role.USER);
            when(userRepository.findById(42L)).thenReturn(Optional.of(target));

            UserResponseDto result = userService.getUserByIdForAdmin(42L, admin);

            assertThat(result.getId()).isEqualTo(42L);
            assertThat(result.getUsername()).isEqualTo("carol");
        }

        @Test
        @DisplayName("throws ForbiddenOperationException for non-admin caller")
        void throwsForNonAdmin() {
            User regularUser = buildUser(1L, "bob", Role.USER);

            assertThatThrownBy(() -> userService.getUserByIdForAdmin(42L, regularUser))
                    .isInstanceOf(ForbiddenOperationException.class);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when target user does not exist")
        void throwsWhenUserNotFound() {
            User admin = buildUser(1L, "admin", Role.ADMIN);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByIdForAdmin(999L, admin))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("setUserEnabled(Long, boolean, User)")
    class SetUserEnabled {

        @Test
        @DisplayName("admin can enable a disabled user")
        void adminEnablesUser() {
            User admin = buildUser(1L, "admin", Role.ADMIN);
            User target = buildUser(2L, "bob", Role.USER);
            target.setEnabled(false);
            when(userRepository.findById(2L)).thenReturn(Optional.of(target));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserResponseDto result = userService.setUserEnabled(2L, true, admin);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().isEnabled()).isTrue();
            assertThat(result.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("admin can disable an active user")
        void adminDisablesUser() {
            User admin = buildUser(1L, "admin", Role.ADMIN);
            User target = buildUser(2L, "bob", Role.USER);
            target.setEnabled(true);
            when(userRepository.findById(2L)).thenReturn(Optional.of(target));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserResponseDto result = userService.setUserEnabled(2L, false, admin);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().isEnabled()).isFalse();
            assertThat(result.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("throws ForbiddenOperationException for non-admin caller")
        void throwsForNonAdmin() {
            User regularUser = buildUser(1L, "bob", Role.USER);

            assertThatThrownBy(() -> userService.setUserEnabled(2L, false, regularUser))
                    .isInstanceOf(ForbiddenOperationException.class);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when target user does not exist")
        void throwsWhenUserNotFound() {
            User admin = buildUser(1L, "admin", Role.ADMIN);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.setUserEnabled(999L, true, admin))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("toggleUserEnabled(Long, User)")
    class ToggleUserEnabled {

        @Test
        @DisplayName("EDGE: toggles enabled=true → false and saves the inverted value")
        void togglesFromEnabledToDisabled() {
            User admin = buildUser(1L, "admin", Role.ADMIN);
            User target = buildUser(2L, "bob", Role.USER);
            target.setEnabled(true);

            when(userRepository.findById(2L)).thenReturn(Optional.of(target));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserResponseDto result = userService.toggleUserEnabled(2L, admin);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().isEnabled()).isFalse();
            assertThat(result.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("EDGE: toggles enabled=false → true and saves the inverted value")
        void togglesFromDisabledToEnabled() {
            User admin = buildUser(1L, "admin", Role.ADMIN);
            User target = buildUser(2L, "bob", Role.USER);
            target.setEnabled(false);

            when(userRepository.findById(2L)).thenReturn(Optional.of(target));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserResponseDto result = userService.toggleUserEnabled(2L, admin);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().isEnabled()).isTrue();
            assertThat(result.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("throws ForbiddenOperationException for non-admin caller")
        void throwsForNonAdmin() {
            User regularUser = buildUser(1L, "bob", Role.USER);

            assertThatThrownBy(() -> userService.toggleUserEnabled(2L, regularUser))
                    .isInstanceOf(ForbiddenOperationException.class);
            verify(userRepository, never()).findById(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when target user does not exist")
        void throwsWhenUserNotFound() {
            User admin = buildUser(1L, "admin", Role.ADMIN);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.toggleUserEnabled(999L, admin))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("DTO mapping (toUserResponseDto)")
    class DtoMapping {

        @ParameterizedTest(name = "role={0}")
        @ValueSource(strings = {"USER", "ADMIN"})
        @DisplayName("maps role field correctly for both enum values")
        void mapsRoleCorrectly(String roleName) {
            Role role = Role.valueOf(roleName);
            User user = buildUser(1L, "alice", role);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserResponseDto result = userService.getCurrentUserProfile(user);

            assertThat(result.getRole()).isEqualTo(role);
        }

        @Test
        @DisplayName("maps enabled=false correctly (disabled account)")
        void mapsDisabledFlag() {
            User user = buildUser(1L, "alice", Role.USER);
            user.setEnabled(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserResponseDto result = userService.getCurrentUserProfile(user);

            assertThat(result.isEnabled()).isFalse();
        }
    }
}
