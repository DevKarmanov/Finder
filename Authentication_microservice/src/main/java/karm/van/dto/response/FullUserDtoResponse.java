package karm.van.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Full user data response")
public record FullUserDtoResponse(
        @Schema(description = "User ID", example = "2")
        Long id,

        @Schema(description = "Username", example = "venik")
        String name,

        @Schema(description = "User's email address", example = "example@mail.com")
        String email,

        @Schema(description = "Roles assigned to the user", example = "[\"USER\"]")
        List<String> role,

        @Schema(description = "User's first name", example = "John")
        String firstName,

        @Schema(description = "User's last name", example = "Doe")
        String lastName,

        @Schema(description = "User's description", example = "Developer at CompanyX")
        String description,

        @Schema(description = "User's country", example = "USA")
        String country,

        @Schema(description = "User's role in command", example = "Team Leader")
        String roleInCommand,

        @Schema(description = "User's skills", example = "Java, Spring, Docker")
        String skills,

        @Schema(description = "Number of followers", example = "120")
        int followerCount,

        @Schema(description = "Number of users the person is following", example = "50")
        int followingCount,

        @Schema(description = "Whether the user is subscribed to this profile", example = "true")
        boolean subscribe,

        @Schema(description = "Whether the user's account is enabled", example = "true")
        boolean isEnable,

        @Schema(description = "User's profile image", implementation = ProfileImageDtoResponse.class)
        ProfileImageDtoResponse profileImage,

        @Schema(description = "List of user cards", implementation = UserCardResponse.class)
        List<UserCardResponse> userCards
) {
}
