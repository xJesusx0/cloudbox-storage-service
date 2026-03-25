package com.github.xjesusx0.cloudbox.application.dtos;

import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Response containing storage usage metrics for a user")
public record StorageUsageResponse(
        @Schema(description = "Total bytes consumed across all requested protocols", example = "11534336")
        long totalBytes,
        
        @Schema(description = "Breakdown of bytes consumed per protocol")
        Map<StorageProtocol, Long> byProtocol
) {}
