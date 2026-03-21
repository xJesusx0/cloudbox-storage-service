package com.github.xjesusx0.cloudbox.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.io.InputStream;

@Builder
@Schema(description = "Represents a file prepared for download, including its content and metadata")
public record FileDownload (
        @Schema(description = "Original filename", example = "manual.pdf")
        String filename,
        @Schema(description = "MIME type of the file content", example = "application/pdf")
        String contentType,
        @Schema(description = "Input stream of the file content (not returned in JSON)", hidden = true)
        InputStream inputStream,
        @Schema(description = "Size of the file in bytes", example = "1048576")
        long size) {
}
