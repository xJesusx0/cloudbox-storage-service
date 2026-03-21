package com.github.xjesusx0.cloudbox.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response containing the JWT token")
public record AuthResponse(
        @Schema(description = "The generated JWT token for authenticating subsequent requests", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token) {
}
