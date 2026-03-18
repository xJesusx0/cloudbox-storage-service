package com.github.xjesusx0.cloudbox.application.services;

import com.github.xjesusx0.cloudbox.application.dtos.UploadFilesRequest;
import com.github.xjesusx0.cloudbox.application.dtos.FileMetadata;
import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<FileMetadata> listFiles(StorageProtocol protocol, String userId) {
        return storageStrategyFactory.get(protocol).listFiles(userId);
    }
}
