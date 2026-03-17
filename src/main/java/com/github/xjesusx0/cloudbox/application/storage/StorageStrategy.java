package com.github.xjesusx0.cloudbox.application.storage;

import com.github.xjesusx0.cloudbox.application.enums.StorageProtocol;
import org.springframework.web.multipart.MultipartFile;

public interface StorageStrategy {
    StorageProtocol getProtocol();
    void save(MultipartFile file);
}
