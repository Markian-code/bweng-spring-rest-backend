package at.technikum.springrestbackend.mapper;

import at.technikum.springrestbackend.dto.UserResponseDto;
import at.technikum.springrestbackend.dto.UserUpdateRequestDto;
import at.technikum.springrestbackend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDto toResponseDto(final User user) {
        if (user == null) {
            return null;
        }

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

    public void updateEntityFromDto(final UserUpdateRequestDto dto, final User user) {
        if (dto == null || user == null) {
            return;
        }

        if (dto.getUsername() != null) {
            user.setUsername(dto.getUsername().trim());
        }

        if (dto.getCountryCode() != null) {
            user.setCountryCode(dto.getCountryCode().trim().toUpperCase());
        }

        if (dto.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(dto.getProfilePictureUrl().trim());
        }
    }
}