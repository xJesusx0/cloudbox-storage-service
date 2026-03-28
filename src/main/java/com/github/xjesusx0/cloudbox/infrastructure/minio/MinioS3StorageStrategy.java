package com.github.xjesusx0.cloudbox.infrastructure.minio;

import com.github.xjesusx0.cloudbox.application.dtos.FileDownload;
import com.github.xjesusx0.cloudbox.core.exceptions.FileDeleteException;
import com.github.xjesusx0.cloudbox.core.exceptions.FileDownloadException;
import com.github.xjesusx0.cloudbox.core.exceptions.FileListException;
import com.github.xjesusx0.cloudbox.core.exceptions.FileUploadException;
import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import com.github.xjesusx0.cloudbox.domain.ports.StorageStrategy;
import io.minio.*;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.StreamSupport;

import com.github.xjesusx0.cloudbox.application.dtos.FileMetadata;

@Slf4j
@Service
public class MinioS3StorageStrategy implements StorageStrategy {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioS3StorageStrategy(
            MinioClient minioClient,
            @Value("${storage.minio.bucket-name}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @PostConstruct
    public void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Bucket '{}' created successfully", bucket);
            } else {
                log.info("Bucket '{}' already exists", bucket);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize MinIO bucket: " + bucket, e);
        }
    }

    @Override
    public StorageProtocol getProtocol() {
        return StorageProtocol.S3;
    }

    @Override
    public void save(MultipartFile file, String userId) {
        try {
            String objectName = userId + "/" + file.getOriginalFilename();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new FileUploadException("Error uploading file to MinIO", e);
        }
    }

    @Override
    public List<FileMetadata> listFiles(String userId) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucket)
                            .prefix(userId + "/")
                            .build());

            return StreamSupport.stream(results.spliterator(), false)
                    .map(result -> {
                        try {
                            Item item = result.get();
                            return FileMetadata.builder()
                                    .name(extractName(item.objectName()))
                                    .path(item.objectName())
                                    .size(item.size())
                                    .isDirectory(item.isDir())
                                    .lastModified(item.lastModified() != null
                                            ? item.lastModified().toInstant()
                                            : null)
                                    .extension(extractExtension(extractName(item.objectName())))
                                    .mimeType(URLConnection.guessContentTypeFromName(item.objectName()))
                                    .etag(item.etag()) // MinIO provee MD5 real
                                    .storageClass(item.storageClass())
                                    .bucketName(bucket)
                                    // versionId disponible con listObjects versioned
                                    // creationTime, lastAccessTime, owner, group: no disponibles en MinIO
                                    .build();
                        } catch (Exception e) {
                            throw new FileListException("Error listing files from MinIO", e);
                        }
                    })
                    .toList();
        } catch (Exception e) {
            throw new FileListException("Error listing files from MinIO", e);
        }
    }

    @Override
    public long getUsedSpace(String userId) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucket)
                            .prefix(userId + "/")
                            .recursive(true)
                            .build());

            return StreamSupport.stream(results.spliterator(), false)
                    .mapToLong(result -> {
                        try {
                            return result.get().size();
                        } catch (Exception e) {
                            log.warn("Error reading item size from MinIO", e);
                            return 0L;
                        }
                    })
                    .sum();
        } catch (Exception e) {
            log.error("Failed to calculate used space for user {} in MinIO", userId, e);
            return 0L;
        }
    }

    @Override
    public FileDownload download(String path) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .build());

            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .build());

            String filename = Paths.get(path).getFileName().toString();
            String contentType = stat.contentType() != null
                    ? stat.contentType()
                    : URLConnection.guessContentTypeFromName(filename);

            return new FileDownload(filename, contentType, stream, stat.size());

        } catch (Exception e) {
            throw new FileDownloadException("Error downloading from MinIO: " + path, e);
        }
    }

    @Override
    public void deleteFile(String path) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .build());
        } catch (Exception e) {
            throw new FileDeleteException("Error deleting file from MinIO: " + path, e);
        }
    }

    private String extractName(String objectName) {
        int lastSlash = objectName.lastIndexOf('/');
        return (lastSlash != -1) ? objectName.substring(lastSlash + 1) : objectName;
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot != -1 && dot < fileName.length() - 1)
                ? fileName.substring(dot + 1).toLowerCase()
                : null;
    }
}
