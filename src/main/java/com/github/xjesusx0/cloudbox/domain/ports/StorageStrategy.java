package com.github.xjesusx0.cloudbox.domain.ports;

import com.github.xjesusx0.cloudbox.application.dtos.FileDownload;
import com.github.xjesusx0.cloudbox.application.dtos.FileMetadata;
import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface StorageStrategy {
    StorageProtocol getProtocol();

    void save(MultipartFile file, String username);

    List<FileMetadata> listFiles(String username);

    FileDownload download(String path);

    long getUsedSpace(String username);

    void deleteFile(String path);
}
