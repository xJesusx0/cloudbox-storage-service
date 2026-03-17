package com.github.xjesusx0.cloudbox.application.models;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UploadFilesRequest (
        @NotNull
        List<UploadItem> files) {
}
