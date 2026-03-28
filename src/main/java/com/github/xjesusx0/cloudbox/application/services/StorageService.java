package com.github.xjesusx0.cloudbox.application.services;

import com.github.xjesusx0.cloudbox.application.dtos.FileDownload;
import com.github.xjesusx0.cloudbox.application.dtos.UploadFilesRequest;
import com.github.xjesusx0.cloudbox.application.dtos.FileMetadata;
import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

import com.github.xjesusx0.cloudbox.application.dtos.StorageUsageResponse;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageStrategyFactory storageStrategyFactory;

    public void save(UploadFilesRequest uploadFilesRequest, String userId) {
        uploadFilesRequest.files().forEach(uploadItem -> {
            uploadItem.protocols().forEach(protocol -> {
                storageStrategyFactory.get(protocol).save(uploadItem.file(), userId);
            });
        });
    }

    public Map<StorageProtocol, List<FileMetadata>> listFiles(Set<StorageProtocol> protocols, String username) {
        return protocols.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        p -> listFiles(p, username)));
    }

    public List<FileMetadata> listFiles(StorageProtocol protocol, String userId) {
        return storageStrategyFactory.get(protocol).listFiles(userId);
    }

    public FileDownload download(StorageProtocol protocol, String path) {
        return storageStrategyFactory
                .get(protocol)
                .download(path);
    }

    public StorageUsageResponse getUsedSpace(Set<StorageProtocol> protocols, String userId) {
        Map<StorageProtocol, CompletableFuture<Long>> futures = protocols.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        p -> CompletableFuture.supplyAsync(() -> storageStrategyFactory.get(p).getUsedSpace(userId))));

        Map<StorageProtocol, Long> byProtocol = futures.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().join()));

        long totalBytes = byProtocol.values().stream().mapToLong(Long::longValue).sum();

        return new StorageUsageResponse(totalBytes, byProtocol);
    }

    public void deleteFile(StorageProtocol protocol, String path) {
        storageStrategyFactory.get(protocol).deleteFile(path);
    }

    public void moveFile(String path, StorageProtocol from, StorageProtocol to) {
        FileDownload file = storageStrategyFactory.get(from).download(path);
        MultipartFile multipartFile = new FileDownloadMultipartFile(file);
        storageStrategyFactory.get(to).save(multipartFile, extractUserId(path));
        storageStrategyFactory.get(from).deleteFile(path);
    }

    /** Extracts the userId from a path of the form "userId/filename". */
    private String extractUserId(String path) {
        int slash = path.indexOf('/');
        return (slash != -1) ? path.substring(0, slash) : path;
    }

    /**
     * Lightweight adapter that exposes a {@link FileDownload} as a {@link MultipartFile}
     * so it can be passed to {@link com.github.xjesusx0.cloudbox.domain.ports.StorageStrategy#save}.
     */
    private record FileDownloadMultipartFile(FileDownload fileDownload) implements MultipartFile {

        @Override public String getName()                  { return fileDownload.filename(); }
        @Override public String getOriginalFilename()      { return fileDownload.filename(); }
        @Override public String getContentType()           { return fileDownload.contentType(); }
        @Override public boolean isEmpty()                 { return fileDownload.size() == 0; }
        @Override public long getSize()                    { return fileDownload.size(); }
        @Override public InputStream getInputStream()      { return fileDownload.inputStream(); }

        @Override
        public byte[] getBytes() throws IOException {
            return fileDownload.inputStream().readAllBytes();
        }

        @Override
        public void transferTo(File dest) throws IOException {
            try (var out = new java.io.FileOutputStream(dest)) {
                fileDownload.inputStream().transferTo(out);
            }
        }
    }
}
