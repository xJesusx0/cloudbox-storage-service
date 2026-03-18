package com.github.xjesusx0.cloudbox.infrastructure.api.controllers;

import com.github.xjesusx0.cloudbox.application.dtos.UploadFilesRequest;
import com.github.xjesusx0.cloudbox.application.dtos.FileMetadata;
import com.github.xjesusx0.cloudbox.application.services.StorageService;
import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File upload and storage operations")
public class FileController {

    private final StorageService storageService;

    @Operation(summary = "Upload files", description = "Uploads one or more files and persists them to the specified storage protocols (S3, FTP, SMB, NFS)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Files uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Object> uploadFile(@Valid @ModelAttribute UploadFilesRequest request) {

        storageService.save(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/protocols")
    public ResponseEntity<List<StorageProtocol>> getAvailableProtocols() {
        return ResponseEntity.ok(List.of(StorageProtocol.values()));
    }

    @Operation(summary = "List files", description = "Lists files from the specified storage protocol")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Files retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid protocol", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<List<FileMetadata>> listFiles(@RequestParam StorageProtocol protocol) {
        return ResponseEntity.ok(storageService.listFiles(protocol));
    }
}
