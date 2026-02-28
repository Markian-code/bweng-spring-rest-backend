package at.technikum.springrestbackend.service;

import at.technikum.springrestbackend.dto.UserResponseDto;
import at.technikum.springrestbackend.dto.UserUpdateRequestDto;
import at.technikum.springrestbackend.entity.Role;
import at.technikum.springrestbackend.entity.User;
import at.technikum.springrestbackend.exception.BadRequestException;
import at.technikum.springrestbackend.exception.ForbiddenOperationException;
import at.technikum.springrestbackend.exception.ResourceNotFoundException;
import at.technikum.springrestbackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserEntityById(final Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));
    }

    public UserResponseDto getCurrentUserProfile(final User currentUser) {
        User user = getUserEntityById(currentUser.getId());
        return toUserResponseDto(user);
    }

    @Transactional
    public UserResponseDto updateCurrentUserProfile(
            final UserUpdateRequestDto request,
            final User currentUser
    ) {
        User user = getUserEntityById(currentUser.getId());

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String newUsername = request.getUsername().trim();
            if (!newUsername.equalsIgnoreCase(user.getUsername())
                    && userRepository.existsByUsername(newUsername)) {
                throw new BadRequestException("Username already taken");
            }
            user.setUsername(newUsername);
        }

        if (request.getCountryCode() != null && !request.getCountryCode().isBlank()) {
            user.setCountryCode(request.getCountryCode().trim().toUpperCase());
        }

        if (request.getProfilePictureUrl() != null) {
            String profilePictureUrl = request.getProfilePictureUrl().trim();
            user.setProfilePictureUrl(profilePictureUrl.isBlank() ? null : profilePictureUrl);
        }

        User saved = userRepository.save(user);
        return toUserResponseDto(saved);
    }

    public List<UserResponseDto> getAllUsersForAdmin(final User currentUser) {
        requireAdmin(currentUser);
        return userRepository.findAll()
                .stream()
                .map(this::toUserResponseDto)
                .toList();
    }

    public UserResponseDto getUserByIdForAdmin(final Long userId, final User currentUser) {
        requireAdmin(currentUser);
        User user = getUserEntityById(userId);
        return toUserResponseDto(user);
    }

    @Transactional
    public UserResponseDto setUserEnabled(
            final Long userId,
            final boolean enabled,
            final User currentUser
    ) {
        requireAdmin(currentUser);
        User user = getUserEntityById(userId);
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        return toUserResponseDto(saved);
    }

    @Transactional
    public UserResponseDto toggleUserEnabled(final Long userId, final User currentUser) {
        requireAdmin(currentUser);
        User user = getUserEntityById(userId);
        user.setEnabled(!user.isEnabled());
        User saved = userRepository.save(user);
        return toUserResponseDto(saved);
    }

    private void requireAdmin(final User currentUser) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("Admin privileges required");
        }
    }

    private UserResponseDto toUserResponseDto(final User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setCountryCode(user.getCountryCode());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());
        dto.setRole(user.getRole());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
