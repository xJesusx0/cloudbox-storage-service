package com.github.xjesusx0.cloudbox.application.models;

import com.github.xjesusx0.cloudbox.application.enums.StorageProtocol;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record UploadItem(
        @NotNull
        MultipartFile file,

        @NotNull @NotEmpty
        List<StorageProtocol> protocols) {
}
