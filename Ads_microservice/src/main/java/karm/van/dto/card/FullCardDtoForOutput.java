package karm.van.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import karm.van.dto.image.ImageDto;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Full information about the card")
public record FullCardDtoForOutput(
        @Schema(description = "Card ID", example = "15")
        Long id,

        @Schema(description = "Card title", example = "Second card")
        String title,

        @Schema(description = "Card text", example = "Description of the second card")
        String text,

        @Schema(description = "Card creation time", example = "2024-10-11")
        LocalDate createTime,

        @Schema(description = "Tags associated with the card", example = "[\"tag1\", \"tag2\"]")
        List<String> tags,

        @Schema(description = "Images associated with the card")
        List<ImageDto> images,

        @Schema(description = "Card author's name", example = "johndoe123456789")
        String authorName
) {}
