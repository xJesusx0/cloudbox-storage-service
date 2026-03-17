package com.github.xjesusx0.cloudbox.application.services;

import com.github.xjesusx0.cloudbox.application.dtos.UploadFilesRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageStrategyFactory storageStrategyFactory;

    public void save(UploadFilesRequest uploadFilesRequest) {
        uploadFilesRequest.files().forEach(uploadItem -> {
            uploadItem.protocols().forEach(protocol -> {
                storageStrategyFactory.get(protocol).save(uploadItem.file());
            });
        });
    }
}
