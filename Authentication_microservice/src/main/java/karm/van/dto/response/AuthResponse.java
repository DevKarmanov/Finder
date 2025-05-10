package karm.van.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response containing JWT and refresh token")
public record AuthResponse(

        @Schema(description = "Access JWT token. Valid for 15 minutes", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String jwtToken,

        @Schema(description = "Refresh token. Valid for 7 days", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refreshToken

) {}

