package karm.van.dto.image;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Image information related to the card")
public record ImageDto(
        @Schema(description = "Image ID", example = "55")
        Long id,

        @Schema(description = "Bucket where the image is stored", example = "images")
        String imageBucket,

        @Schema(description = "Name of the image file", example = "image-name1.jpg")
        String imageName
) {}
