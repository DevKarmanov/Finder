package karm.van.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication request")
public record AuthRequest(
        @Schema(description = "User's login or username", example = "johndoe123456789")
        String username,

        @Schema(description = "User's password", example = "securePassword!2024")
        String password
) {}
