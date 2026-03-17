package com.github.xjesusx0.cloudbox.application.enums;

import com.github.xjesusx0.cloudbox.exceptions.NotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum StorageProtocol {

    S3(1),
    FTP(2),
    SMB(3),
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