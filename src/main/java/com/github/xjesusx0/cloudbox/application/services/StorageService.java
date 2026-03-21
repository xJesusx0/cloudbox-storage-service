package com.github.xjesusx0.cloudbox.application.services;

import com.github.xjesusx0.cloudbox.application.dtos.FileDownload;
import com.github.xjesusx0.cloudbox.application.dtos.UploadFilesRequest;
import com.github.xjesusx0.cloudbox.application.dtos.FileMetadata;
import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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
                        p -> listFiles(p, username)
                ));
    }

    public List<FileMetadata> listFiles(StorageProtocol protocol, String userId) {
        return storageStrategyFactory.get(protocol).listFiles(userId);
    }

    public FileDownload download(StorageProtocol protocol, String path){
        return storageStrategyFactory
                .get(protocol)
                .download(path);
    }
}
