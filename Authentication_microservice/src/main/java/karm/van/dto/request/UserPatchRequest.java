package karm.van.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Optional;

@Schema(description = "Request body for patching user details. Fields are optional.")
public record UserPatchRequest(
        @Schema(description = "The username of the user", example = "newUsername")
        Optional<String> name,

        @Schema(description = "The email address of the user", example = "newemail@example.com")
        Optional<String> email,

        @Schema(description = "The first name of the user", example = "John")
        Optional<String> firstName,

        @Schema(description = "The last name of the user", example = "Doe")
        Optional<String> lastName,

        @Schema(description = "A short description of the user", example = "A passionate developer")
        Optional<String> description,

        @Schema(description = "The user's country of residence", example = "USA")
        Optional<String> country,

        @Schema(description = "The role in the user's organization", example = "Lead Developer")
        Optional<String> roleInCommand,

        @Schema(description = "The skills possessed by the user", example = "Java, Spring Boot, Docker")
        Optional<String> skills
) {
}
