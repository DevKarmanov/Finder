package karm.van.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User DTO for search response")
public record UserDtoForSearchResponse(
        @Schema(description = "Unique identifier of the user", example = "1") Long id,
        @Schema(description = "Login of the user", example = "johndoe") String name,
        @Schema(description = "User's first name", example = "John") String firstName,
        @Schema(description = "User's last name", example = "Doe") String lastName,
        @Schema(description = "Short description about the user", example = "A passionate developer") String description,
        @Schema(description = "Country where the user is located", example = "USA") String country,
        @Schema(description = "Role in command or organization", example = "Team Lead") String roleInCommand,
        @Schema(description = "Skills that the user possesses", example = "Java, Spring, Microservices") String skills
) {}
