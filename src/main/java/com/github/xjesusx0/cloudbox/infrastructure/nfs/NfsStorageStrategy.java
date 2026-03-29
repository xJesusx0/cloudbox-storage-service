package com.github.xjesusx0.cloudbox.infrastructure.nfs;

import com.github.xjesusx0.cloudbox.application.dtos.FileDownload;
import com.github.xjesusx0.cloudbox.application.dtos.FileMetadata;
import com.github.xjesusx0.cloudbox.core.exceptions.FileDeleteException;
import com.github.xjesusx0.cloudbox.core.exceptions.FileDownloadException;
import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import com.github.xjesusx0.cloudbox.domain.ports.StorageStrategy;
import com.github.xjesusx0.cloudbox.core.exceptions.FileListException;
import com.github.xjesusx0.cloudbox.core.exceptions.FileUploadException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.List;

@Slf4j
@Service
public class NfsStorageStrategy implements StorageStrategy {

    private final String nfsMountPath;

    public NfsStorageStrategy(
            @Value("${storage.nfs.mount-path}") String nfsMountPath) {
        this.nfsMountPath = nfsMountPath;
    }

    @Override
    public StorageProtocol getProtocol() {
        return StorageProtocol.NFS;
    }

    @Override
    public void save(MultipartFile file, String userId) {
        Path userDir = Paths.get(nfsMountPath, userId);

        try {
            Files.createDirectories(userDir);
            Path destination = userDir.resolve(file.getOriginalFilename());
            file.transferTo(destination);
        } catch (IOException e) {
            throw new FileUploadException("Error guardando en NFS", e);
        }
    }

    @Override
    public List<FileMetadata> listFiles(String userId) {
        Path userDir = Paths.get(nfsMountPath, userId);

        try {
            if (!Files.exists(userDir))
                return List.of();

            return Files.list(userDir)
                    .map(this::buildMetadata)
                    .toList();
        } catch (IOException e) {
            throw new FileListException("Error listando archivos NFS", e);
        }
    }

    @Override
    public long getUsedSpace(String userId) {
        Path userDir = Paths.get(nfsMountPath, userId);

        if (!Files.exists(userDir)) {
            return 0L;
        }

        try (java.util.stream.Stream<Path> stream = Files.walk(userDir)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            log.warn("No se pudo leer el tamaño de: {}", path, e);
                            return 0L;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            log.error("Error calculando el espacio usado del usuario {} en NFS", userId, e);
            return 0L;
        }
    }

    @Override
    public FileDownload download(String path) {
        try {
            Path filePath = Paths.get(nfsMountPath, path);

            if (!Files.exists(filePath)) {
                throw new FileDownloadException("File not found: " + path, null);
            }

            String filename = filePath.getFileName().toString();
            String contentType = Files.probeContentType(filePath);
            if (contentType == null)
                contentType = URLConnection.guessContentTypeFromName(filename);

            // NFS es local para Java — el stream puede vivir fuera sin problema
            return new FileDownload(
                    filename,
                    contentType,
                    Files.newInputStream(filePath),
                    Files.size(filePath));

        } catch (IOException e) {
            throw new FileDownloadException("Error downloading from NFS: " + path, e);
        }
    }

    @Override
    public void deleteFile(String path) {
        try {
            Path filePath = Paths.get(path); // el path ya incluye el punto de montaje
            boolean deleted = Files.deleteIfExists(filePath);
            if (!deleted) {
                throw new FileDeleteException("File not found in NFS: " + path);
            }
        } catch (IOException e) {
            throw new FileDeleteException("Error deleting file from NFS: " + path, e);
        }
    }

    private FileMetadata buildMetadata(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            String fileName = path.getFileName().toString();
            PosixFileAttributes posix = Files.readAttributes(path, PosixFileAttributes.class); // Linux

            return FileMetadata.builder()
                    .name(fileName)
                    .path(path.toAbsolutePath().toString())
                    .size(attrs.size())
                    .isDirectory(attrs.isDirectory())
                    .lastModified(attrs.lastModifiedTime().toInstant())
                    .creationTime(attrs.creationTime().toInstant())
                    .lastAccessTime(attrs.lastAccessTime().toInstant())
                    .extension(extractExtension(fileName))
                    .mimeType(Files.probeContentType(path))
                    .etag(attrs.size() + "-" + attrs.lastModifiedTime().toMillis())
                    .owner(posix.owner().getName()) // solo Linux
                    .group(posix.group().getName()) // solo Linux
                    .build();

        } catch (IOException e) {
            throw new FileListException("Error leyendo metadatos de: " + path, e);
        }
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex != -1 && dotIndex < fileName.length() - 1)
                ? fileName.substring(dotIndex + 1).toLowerCase()
                : null;
    }

    private String buildEtag(BasicFileAttributes attrs) {
        return attrs.size() + "-" + attrs.lastModifiedTime().toMillis();
    }
}
