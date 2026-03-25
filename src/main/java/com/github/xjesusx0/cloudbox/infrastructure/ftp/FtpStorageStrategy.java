package com.github.xjesusx0.cloudbox.infrastructure.ftp;

import com.github.xjesusx0.cloudbox.application.dtos.FileDownload;
import com.github.xjesusx0.cloudbox.application.dtos.FileMetadata;
import com.github.xjesusx0.cloudbox.core.exceptions.FileDownloadException;
import com.github.xjesusx0.cloudbox.core.exceptions.FileListException;
import com.github.xjesusx0.cloudbox.core.exceptions.FileUploadException;
import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import com.github.xjesusx0.cloudbox.domain.ports.StorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.PrintCommandListener;

import java.io.*;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

@Slf4j
@Service
public class FtpStorageStrategy implements StorageStrategy {

    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public FtpStorageStrategy(
            @Value("${storage.ftp.host}") String host,
            @Value("${storage.ftp.port:21}") int port,
            @Value("${storage.ftp.username}") String username,
            @Value("${storage.ftp.password}") String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @Override
    public StorageProtocol getProtocol() {
        return StorageProtocol.FTP;
    }

    @Override
    public void save(MultipartFile file, String userId) {
        FTPSClient ftpClient = createFtpClient();
        ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true));
        try {
            ftpClient.connect(host, port);
            if (!ftpClient.login(username, password)) {
                throw new FileUploadException("Could not login to FTP server",
                        new IOException("Authentication Failed"));
            }

            ftpClient.execPBSZ(0);
            ftpClient.execPROT("P");

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Create user directory if it doesn't exist
            ensureDirectoryExists(ftpClient, userId);

            String remoteFilePath = userId + "/" + file.getOriginalFilename();
            try (InputStream inputStream = file.getInputStream()) {
                boolean uploaded = ftpClient.storeFile(remoteFilePath, inputStream);
                if (!uploaded) {
                    throw new FileUploadException("Failed to upload file to FTP server as " + remoteFilePath,
                            new IOException("Upload failed"));
                }
            }
        } catch (IOException e) {
            throw new FileUploadException("Error uploading file to FTP server", e);
        } finally {
            disconnect(ftpClient);
        }
    }

    @Override
    public List<FileMetadata> listFiles(String userId) {
        FTPSClient ftpClient = createFtpClient();
        ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true));
        try {
            ftpClient.connect(host, port);
            if (!ftpClient.login(username, password)) {
                throw new FileListException("Could not login to FTP server", new IOException("Authentication Failed"));
            }
            ftpClient.execPBSZ(0);
            ftpClient.execPROT("P");
            ftpClient.enterLocalPassiveMode();

            if (!ftpClient.changeWorkingDirectory(userId)) {
                return List.of();
            }

            FTPFile[] files = ftpClient.listFiles();

            return Arrays.stream(files)
                    .filter(FTPFile::isValid) // descartar entradas que fallaron al parsear
                    .map(file -> buildMetadata(file, userId))
                    .toList();

        } catch (IOException e) {
            throw new FileListException("Error listing files from FTP server", e);
        } finally {
            disconnect(ftpClient);
        }
    }

    @Override
    public long getUsedSpace(String userId) {
        FTPSClient ftpClient = createFtpClient();
        try {
            ftpClient.connect(host, port);
            if (!ftpClient.login(username, password)) {
                log.warn("Could not login to FTP server for used space calculation");
                return 0L;
            }
            ftpClient.execPBSZ(0);
            ftpClient.execPROT("P");
            ftpClient.enterLocalPassiveMode();

            if (!ftpClient.changeWorkingDirectory(userId)) {
                return 0L;
            }

            return calculateFtpSize(ftpClient, "");
        } catch (IOException e) {
            log.error("Error calculating used space for user {} on FTP server", userId, e);
            return 0L;
        } finally {
            disconnect(ftpClient);
        }
    }

    private long calculateFtpSize(FTPSClient ftpClient, String path) throws IOException {
        long totalSize = 0;
        FTPFile[] files = path.isEmpty() ? ftpClient.listFiles() : ftpClient.listFiles(path);

        if (files != null) {
            for (FTPFile file : files) {
                if (file.isValid() && !file.getName().equals(".") && !file.getName().equals("..")) {
                    if (file.isDirectory()) {
                        String subDir = path.isEmpty() ? file.getName() : path + "/" + file.getName();
                        totalSize += calculateFtpSize(ftpClient, subDir);
                    } else {
                        totalSize += file.getSize();
                    }
                }
            }
        }
        return totalSize;
    }

    @Override
    public FileDownload download(String path) {
        FTPSClient ftpClient = createFtpClient();
        try {
            ftpClient.connect(host, port);
            if (!ftpClient.login(username, password)) {
                throw new FileDownloadException("FTP authentication failed", null);
            }
            ftpClient.execPBSZ(0);
            ftpClient.execPROT("P");
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // ⚠️ FTP: hay que volcar a memoria antes de cerrar la conexión
            // Si devolvemos el stream directamente, la conexión se cierra
            // en el finally y el controller no puede leer nada
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            boolean success = ftpClient.retrieveFile(path, buffer);

            if (!success) {
                throw new FileDownloadException("File not found in FTP: " + path, null);
            }

            String filename = Paths.get(path).getFileName().toString();
            String contentType = URLConnection.guessContentTypeFromName(filename);
            byte[] bytes = buffer.toByteArray();

            return new FileDownload(
                    filename,
                    contentType != null ? contentType : "application/octet-stream",
                    new ByteArrayInputStream(bytes),
                    bytes.length
            );

        } catch (IOException e) {
            throw new FileDownloadException("Error downloading from FTP: " + path, e);
        } finally {
            disconnect(ftpClient);
        }
    }

    private FileMetadata buildMetadata(FTPFile file, String userId) {
        return FileMetadata.builder()
                .name(file.getName())
                .path(userId + "/" + file.getName())
                .size(file.getSize())
                .isDirectory(file.isDirectory())
                .lastModified(toInstant(file.getTimestamp()))
                .extension(extractExtension(file.getName()))
                .mimeType(URLConnection.guessContentTypeFromName(file.getName()))
                .etag(file.getSize() + "-" + toInstant(file.getTimestamp()).toEpochMilli())
                .owner(file.getUser())
                .group(file.getGroup())
                // creationTime y lastAccessTime: no disponibles en FTP
                // allocationSize: no disponible en FTP
                .build();
    }

    private Instant toInstant(Calendar calendar) {
        return calendar.toInstant();
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot != -1 && dot < fileName.length() - 1)
                ? fileName.substring(dot + 1).toLowerCase()
                : null;
    }

    private void ensureDirectoryExists(FTPSClient ftpClient, String dirPath) throws IOException {
        String currentDir = ftpClient.printWorkingDirectory();
        String[] pathElements = dirPath.split("/");

        for (String element : pathElements) {
            if (element.isEmpty()) {
                continue;
            }
            if (!ftpClient.changeWorkingDirectory(element)) {
                if (!ftpClient.makeDirectory(element)) {
                    throw new IOException("Unable to create directory: " + element);
                }
                if (!ftpClient.changeWorkingDirectory(element)) {
                    throw new IOException("Unable to change into newly created directory: " + element);
                }
            }
        }
        // go back to root dir after creation to avoid breaking subsequent paths
        ftpClient.changeWorkingDirectory(currentDir);
    }

    private void disconnect(FTPSClient ftpClient) {
        if (ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException ex) {
                log.warn("Error disconnecting from FTP server", ex);
            }
        }
    }

    protected FTPSClient createFtpClient() {
        FTPSClient ftpClient = new FTPSClient();
        ftpClient.setEndpointCheckingEnabled(false);
        ftpClient.setTrustManager(org.apache.commons.net.util.TrustManagerUtils.getAcceptAllTrustManager());
        return ftpClient;
    }
}
