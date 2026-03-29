package com.github.xjesusx0.cloudbox.infrastructure.api.controllers;

import com.github.xjesusx0.cloudbox.application.dtos.FileDownload;
import com.github.xjesusx0.cloudbox.application.dtos.UploadFilesRequest;
import com.github.xjesusx0.cloudbox.application.dtos.FileMetadata;
import com.github.xjesusx0.cloudbox.application.services.StorageService;
import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import com.github.xjesusx0.cloudbox.application.dtos.StorageUsageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "File Management", description = "Operations for uploading, listing, and downloading files across multiple storage backends (S3, FTP, SMB, NFS)")
public class FileController {

    private final StorageService storageService;

    @Operation(
            summary = "Upload one or more files",
            description = "Uploads multiple files and distributes them across the specified storage protocols. Each file can be sent to multiple backends simultaneously."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Files successfully uploaded and persisted to all requested storage backends"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request: Invalid file data, missing protocols, or validation failure",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized: Authentication token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error: Failure during the upload or storage process",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> uploadFile(
            @Valid @ModelAttribute UploadFilesRequest request,
            Principal principal) {

        storageService.save(request, principal.getName());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get available storage protocols",
            description = "Returns a list of all storage protocols currently supported by the system (e.g., S3, FTP, SMB, NFS)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of protocols retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = StorageProtocol.class)))
            )
    })
    @GetMapping("/protocols")
    public ResponseEntity<List<StorageProtocol>> getAvailableProtocols() {
        return ResponseEntity.ok(List.of(StorageProtocol.values()));
    }

    @Operation(
            summary = "List files from storage",
            description = "Retrieves a comprehensive list of files and metadata from the specified storage protocols for the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "File list retrieved successfully, grouped by protocol",
                    content = @Content(schema = @Schema(description = "Map of protocols to their respective file lists"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request: One or more requested protocols are invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping
    public ResponseEntity<Map<StorageProtocol, List<FileMetadata>>> listFiles(
            @Parameter(description = "Set of storage protocols to query for files", required = true, example = "S3,FTP")
            @RequestParam Set<StorageProtocol> protocols,
            Principal principal) {

        return ResponseEntity.ok(storageService.listFiles(protocols, principal.getName()));
    }

    @Operation(
            summary = "Download a file",
            description = "Downloads a specific file from a storage backend given its path and the protocol used to store it."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "File stream retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found: The requested file path does not exist on the specified protocol",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request: Invalid path or protocol",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error during file retrieval",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> download(
            @Parameter(description = "The relative path of the file to download", required = true, example = "documents/manual.pdf")
            @RequestParam String path,
            @Parameter(description = "The storage protocol where the file is located", required = true, example = "S3")
            @RequestParam StorageProtocol protocol) {

        FileDownload file = storageService.download(protocol, path);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.size())
                .body(new InputStreamResource(file.inputStream()));
    }

    @Operation(
            summary = "Get storage usage",
            description = "Retrieves the total and per-protocol storage space consumed by the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Storage usage retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StorageUsageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request: One or more requested protocols are invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/usage")
    public ResponseEntity<StorageUsageResponse> getUsedSpace(
            @Parameter(description = "Set of storage protocols to query for usage", required = true, example = "S3,FTP")
            @RequestParam Set<StorageProtocol> protocols,
            Principal principal) {

        return ResponseEntity.ok(storageService.getUsedSpace(protocols, principal.getName()));
    }

    @Operation(
            summary = "Delete a file",
            description = "Permanently deletes a file from the specified storage backend given its path."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "File deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found: The file does not exist at the specified path",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request: Invalid path or protocol",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error during deletion",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "Relative path of the file to delete", required = true, example = "userId/document.pdf")
            @RequestParam String path,
            @Parameter(description = "Storage protocol where the file resides", required = true, example = "S3")
            @RequestParam StorageProtocol protocol) {

        storageService.deleteFile(protocol, path);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Move a file between protocols",
            description = "Downloads a file from the source protocol, uploads it to the destination protocol, and deletes it from the source."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "File moved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found: The file does not exist at the specified path in the source protocol",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request: Invalid path or protocol",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error during move operation",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PostMapping("/move")
    public ResponseEntity<Void> moveFile(
            @Parameter(description = "Relative path of the file to move (e.g. userId/filename.pdf)", required = true)
            @RequestParam String path,
            @Parameter(description = "Source storage protocol", required = true, example = "FTP")
            @RequestParam StorageProtocol from,
            @Parameter(description = "Destination storage protocol", required = true, example = "S3")
            @RequestParam StorageProtocol to,
            Principal principal) {

        storageService.moveFile(path, from, to, principal.getName());
        return ResponseEntity.noContent().build();
    }
}

