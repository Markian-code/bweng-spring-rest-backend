package at.technikum.springrestbackend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserUpdateRequestDto {

    @Size(min = 5, max = 50, message = "Username must be between 5 and 50 characters")
    private String username;

    @Pattern(regexp = "^[A-Za-z]{2}$", message = "Country code must be a valid 2-letter code")
    private String countryCode;

    @Size(max = 1000, message = "Profile picture URL must not exceed 1000 characters")
    private String profilePictureUrl;

    public UserUpdateRequestDto() {
    }

    public String getUsername() {
        return username;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    public void setProfilePictureUrl(final String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}