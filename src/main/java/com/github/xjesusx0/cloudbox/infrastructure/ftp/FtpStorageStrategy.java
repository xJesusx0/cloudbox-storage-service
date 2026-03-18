package com.github.xjesusx0.cloudbox.infrastructure.ftp;

import com.github.xjesusx0.cloudbox.application.dtos.FileMetadata;
import com.github.xjesusx0.cloudbox.core.exceptions.FileListException;
import com.github.xjesusx0.cloudbox.core.exceptions.FileUploadException;
import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import com.github.xjesusx0.cloudbox.domain.ports.StorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.PrintCommandListener;
import java.io.PrintWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
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

            // Try to change working directory to test if it exists
            if (!ftpClient.changeWorkingDirectory(userId)) {
                // If it doesn't exist, simply return empty list
                return List.of();
            }

            FTPFile[] files = ftpClient.listFiles();

            return Arrays.stream(files)
                    .map(file -> FileMetadata.builder()
                            .name(file.getName())
                            .path(userId + "/" + file.getName())
                            .size(file.getSize())
                            .isDirectory(file.isDirectory())
                            .lastModified(file.getTimestamp() != null ? file.getTimestamp().toInstant() : null)
                            // FTP doesn't natively supply ETag
                            .build())
                    .toList();

        } catch (IOException e) {
            throw new FileListException("Error listing files from FTP server", e);
        } finally {
            disconnect(ftpClient);
        }
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
