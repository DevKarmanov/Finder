package karm.van.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO representing the comment text")
public record CommentDto(
        @Schema(description = "The content of the comment", example = "This is a comment")
        String text
) {
}
