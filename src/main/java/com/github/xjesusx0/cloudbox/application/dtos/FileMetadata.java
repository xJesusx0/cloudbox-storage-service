package com.github.xjesusx0.cloudbox.application.dtos;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FileMetadata {

    private String name;
    private String path;
    private long size;
    private boolean isDirectory;
    private Instant lastModified;

    // opcionales
    private String extension;
    private String mimeType;

    private String etag;
    private String url;

}