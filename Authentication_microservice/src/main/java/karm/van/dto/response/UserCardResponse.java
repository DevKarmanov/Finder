package karm.van.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Information about a user's card")
public record UserCardResponse(
        @Schema(description = "The ID of the user card", example = "1")
        Long id,

        @Schema(description = "The title of the user card", example = "Project A")
        String title,

        @Schema(description = "The text content of the user card", example = "A brief description of Project A.")
        String text
) {
}
