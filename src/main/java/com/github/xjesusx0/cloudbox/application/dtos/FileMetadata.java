package com.github.xjesusx0.cloudbox.application.dtos;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Detailed information and metadata of a file or directory")
public class FileMetadata {

    // ── Comunes a todos los protocolos ──────────────────────────────────────

    @Schema(description = "Filename", example = "report.pdf")
    private String name;

    @Schema(description = "Relative path to the user's root", example = "documents/reports/report.pdf")
    private String path;

    @Schema(description = "File size in bytes", example = "102400")
    private long size;

    @Schema(description = "Whether the item is a directory", example = "false")
    private boolean isDirectory;

    @Schema(description = "Last modified timestamp")
    private Instant lastModified;

    // ── Identificación ──────────────────────────────────────────────────────

    @Schema(description = "Entity tag for cache and versioning", example = "5eb63bb0-123")
    private String etag;

    @Schema(description = "Public or pre-signed URL for the file", example = "https://storage.example.com/file.pdf")
    private String url;

    // ── Tipo de archivo ─────────────────────────────────────────────────────

    @Schema(description = "File extension without the dot", example = "pdf")
    private String extension;

    @Schema(description = "MIME type of the file", example = "application/pdf")
    private String mimeType;

    // ── Tiempos extendidos ──────────────────────────────────────────────────

    @Schema(description = "File creation timestamp")
    private Instant creationTime;

    @Schema(description = "Last access timestamp")
    private Instant lastAccessTime;

    // ── Propiedad (Unix/Posix) ───────────────────────────────────────────────

    @Schema(description = "Owner of the file", example = "cloud-user")
    private String owner;

    @Schema(description = "Group owner of the file", example = "staff")
    private String group;

    // ── Espacio en disco ────────────────────────────────────────────────────

    @Schema(description = "Actual disk space occupied (may be greater than logical size)", example = "106496")
    private Long allocationSize;

    // ── MinIO / S3 ──────────────────────────────────────────────────────────

    @Schema(description = "Storage class (S3/MinIO only)", example = "STANDARD")
    private String storageClass;

    @Schema(description = "Version ID (if versioning enabled)", example = "v1")
    private String versionId;

    @Schema(description = "Source bucket name (S3/MinIO only)", example = "my-bucket")
    private String bucketName;

}