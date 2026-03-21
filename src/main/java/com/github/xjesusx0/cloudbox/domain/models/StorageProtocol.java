package com.github.xjesusx0.cloudbox.domain.models;

import com.github.xjesusx0.cloudbox.core.exceptions.NotFoundException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
@Schema(description = "Supported storage protocols for file operations")
public enum StorageProtocol {

    @Schema(description = "S3 / Object Storage (MinIO)")
    S3(1),
    @Schema(description = "File Transfer Protocol")
    FTP(2),
    @Schema(description = "Server Message Block (Windows Shared Folder)")
    SMB(3),
    @Schema(description = "Network File System")
    NFS(4);

    private final int id;

    public boolean isObjectStorage() {
        return this == S3;
    }

    public boolean isFileSystem() {
        return this == SMB || this == NFS || this == FTP;
    }

    public static StorageProtocol fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new NotFoundException("Protocol code cannot be null or empty");
        }

        return Arrays.stream(values())
                .filter(p -> p.name().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Unsupported protocol: " + code));
    }

    public static StorageProtocol fromId(int id) {
        return Arrays.stream(values())
                .filter(p -> p.id == id)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Unsupported protocol id: " + id));
    }
}
