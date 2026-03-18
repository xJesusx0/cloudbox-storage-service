package com.github.xjesusx0.cloudbox.infrastructure.minio;

import com.github.xjesusx0.cloudbox.core.exceptions.FileListException;
import com.github.xjesusx0.cloudbox.core.exceptions.FileUploadException;
import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import com.github.xjesusx0.cloudbox.domain.ports.StorageStrategy;
import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
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
    public void save(MultipartFile file) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(file.getOriginalFilename())
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new FileUploadException("Error uploading file to MinIO", e);
        }
    }

    @Override
    public List<FileMetadata> listFiles() {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucket).build());

            return StreamSupport.stream(results.spliterator(), false)
                    .map(result -> {
                        try {
                            Item item = result.get();
                            return FileMetadata.builder()
                                    .name(item.objectName())
                                    .path(item.objectName())
                                    .size(item.size())
                                    .isDirectory(item.isDir())
                                    .lastModified(item.lastModified() != null ? item.lastModified().toInstant() : null)
                                    .etag(item.etag())
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
}
