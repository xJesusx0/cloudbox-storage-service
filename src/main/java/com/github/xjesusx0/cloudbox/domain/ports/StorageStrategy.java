package com.github.xjesusx0.cloudbox.domain.ports;

import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import org.springframework.web.multipart.MultipartFile;

public interface StorageStrategy {
    StorageProtocol getProtocol();
    void save(MultipartFile file);
}
