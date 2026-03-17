package com.github.xjesusx0.cloudbox.controllers;

import com.github.xjesusx0.cloudbox.application.models.UploadFilesRequest;
import com.github.xjesusx0.cloudbox.application.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final StorageService storageService;

    @PostMapping
    public ResponseEntity<Object> uploadFile(@ModelAttribute UploadFilesRequest request) {

        storageService.save(request);
        return ResponseEntity.ok().build();
    }
}