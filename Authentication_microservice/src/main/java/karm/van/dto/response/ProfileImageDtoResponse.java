package karm.van.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Details of the user's profile image")
public record ProfileImageDtoResponse(
        @Schema(description = "The file name of the profile image", example = "profile_picture.jpg")
        String imageName,

        @Schema(description = "The name of the bucket where the profile image is stored", example = "user-profile-images")
        String bucketName
) {
}
