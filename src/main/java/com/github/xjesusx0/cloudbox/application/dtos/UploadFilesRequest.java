package com.github.xjesusx0.cloudbox.application.dtos;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Request to upload one or more files to the configured storage backends")
public record UploadFilesRequest(

        @ArraySchema(schema = @Schema(implementation = UploadItem.class),
                minItems = 1)
        @NotNull
        List<@Valid UploadItem> files) {
}
