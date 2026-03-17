package com.github.xjesusx0.cloudbox.application.dtos;

import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Schema(description = "Represents a single file and the storage protocols where it should be persisted")
public record UploadItem(

        @Schema(description = "The file to upload", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        MultipartFile file,

        @ArraySchema(schema = @Schema(implementation = StorageProtocol.class,
                description = "Target storage protocol"),
                minItems = 1)
        @Schema(description = "List of storage protocols to persist the file to (e.g. S3, FTP, SMB, NFS)")
        @NotNull @NotEmpty
        List<StorageProtocol> protocols) {
}
