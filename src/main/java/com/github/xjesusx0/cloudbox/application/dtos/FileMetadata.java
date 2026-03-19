package com.github.xjesusx0.cloudbox.application.dtos;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileMetadata {

    // ── Comunes a todos los protocolos ──────────────────────────────────────

    private String name; // nombre del archivo
    private String path; // ruta relativa al usuario
    private long size; // tamaño lógico en bytes
    private boolean isDirectory;
    private Instant lastModified; // última escritura

    // ── Identificación ──────────────────────────────────────────────────────

    private String etag; // size-lastModifiedMillis (local/FTP/SMB)
                         // o MD5 real (MinIO/S3)
    private String url; // URL pública o presignada (MinIO)

    // ── Tipo de archivo ─────────────────────────────────────────────────────

    private String extension; // "pdf", "jpg", null si directorio
    private String mimeType; // "application/pdf", null si desconocido

    // ── Tiempos extendidos ──────────────────────────────────────────────────
    // Disponible en: Local (BasicFileAttributes), SMB, FTP parcialmente
    // NO disponible en: MinIO (solo lastModified)

    private Instant creationTime; // cuándo se creó
    private Instant lastAccessTime; // último acceso/lectura

    // ── Propiedad (Unix/Posix) ───────────────────────────────────────────────
    // Disponible en: Local (PosixFileAttributes), FTP, SMB
    // NO disponible en: MinIO nativamente

    private String owner; // usuario dueño del archivo
    private String group; // grupo dueño

    // ── Espacio en disco ────────────────────────────────────────────────────
    // Disponible en: SMB (allocationSize), Local (toFile().getTotalSpace())
    // NO disponible en: MinIO, FTP

    private Long allocationSize; // espacio real ocupado en disco (>= size)

    // ── MinIO / S3 ──────────────────────────────────────────────────────────
    // Solo disponibles con almacenamiento tipo objeto

    private String storageClass; // "STANDARD", "REDUCED_REDUNDANCY", etc.
    private String versionId; // si el bucket tiene versionado habilitado
    private String bucketName; // nombre del bucket de origen

}