package karm.van.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CardDto(
        @Schema(description = "Unique identifier of the card")
        Long id,

        @Schema(description = "Title of the card", example = "Awesome Card")
        String title,

        @Schema(description = "Text or description of the card", example = "This is a detailed description of the card.")
        String text,

        @Schema(description = "Tags associated with the card", example = "[\"tag1\", \"tag2\"]")
        List<String> tags
) {}
