package com.github.xjesusx0.cloudbox.application.dtos;

import lombok.Builder;
import java.io.InputStream;

@Builder
public record FileDownload (
        String filename, String contentType,
        InputStream inputStream, long size) {
}
