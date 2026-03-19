package com.github.xjesusx0.cloudbox.infrastructure.smb;

import com.github.xjesusx0.cloudbox.application.dtos.FileMetadata;
import com.github.xjesusx0.cloudbox.core.exceptions.FileListException;
import com.github.xjesusx0.cloudbox.core.exceptions.FileUploadException;
import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import com.github.xjesusx0.cloudbox.domain.ports.StorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.share.File;

import java.io.OutputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;

@Slf4j
@Service
public class SmbStorageStrategy implements StorageStrategy {

    private final String host;
    private final String share;
    private final String username;
    private final String password;
    private final String domain;

    public SmbStorageStrategy(
            @Value("${storage.smb.host}") String host,
            @Value("${storage.smb.share}") String share,
            @Value("${storage.smb.username}") String username,
            @Value("${storage.smb.password}") String password,
            @Value("${storage.smb.domain:}") String domain) {
        this.host = host;
        this.share = share;
        this.username = username;
        this.password = password;
        this.domain = domain;
    }

    @Override
    public StorageProtocol getProtocol() {
        return StorageProtocol.SMB;
    }

    @Override
    public void save(MultipartFile file, String userId) {
        SMBClient client = createSmbClient();
        try (Connection connection = client.connect(host)) {
            AuthenticationContext ac = new AuthenticationContext(username, password.toCharArray(), domain);
            try (Session session = connection.authenticate(ac)) {
                try (DiskShare diskShare = (DiskShare) session.connectShare(share)) {
                    
                    ensureDirectoryExists(diskShare, userId);

                    String remoteFilePath = userId + "\\" + file.getOriginalFilename();
                    
                    try (File smbFile = diskShare.openFile(
                            remoteFilePath,
                            EnumSet.of(AccessMask.GENERIC_WRITE),
                            null,
                            SMB2ShareAccess.ALL,
                            SMB2CreateDisposition.FILE_OVERWRITE_IF,
                            null)) {
                        
                        try (OutputStream os = smbFile.getOutputStream()) {
                            file.getInputStream().transferTo(os);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new FileUploadException("Error uploading file to SMB server", e);
        } finally {
            client.close();
        }
    }

    @Override
    public List<FileMetadata> listFiles(String userId) {
        SMBClient client = createSmbClient();
        try (Connection connection = client.connect(host)) {
            AuthenticationContext ac = new AuthenticationContext(username, password.toCharArray(), domain);
            try (Session session = connection.authenticate(ac)) {
                try (DiskShare diskShare = (DiskShare) session.connectShare(share)) {
                    
                    if (!diskShare.folderExists(userId)) {
                        return Collections.emptyList();
                    }

                    List<FileIdBothDirectoryInformation> list = diskShare.list(userId);
                    return list.stream()
                            .filter(f -> !f.getFileName().equals(".") && !f.getFileName().equals(".."))
                            .map(f -> FileMetadata.builder()
                                    .name(f.getFileName())
                                    .path(userId + "/" + f.getFileName())
                                    .size(f.getEndOfFile())
                                    .isDirectory((f.getFileAttributes() & com.hierynomus.msfscc.FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue()) != 0)
                                    .lastModified(f.getLastWriteTime() != null ? f.getLastWriteTime().toInstant() : null)
                                    .build())
                            .collect(Collectors.toList());
                    
                }
            }
        } catch (Exception e) {
            throw new FileListException("Error listing files from SMB server", e);
        } finally {
            client.close();
        }
    }

    private void ensureDirectoryExists(DiskShare diskShare, String dirPath) {
        if (!diskShare.folderExists(dirPath)) {
            String[] pathElements = dirPath.split("\\\\|/");
            String currentPath = "";
            for (String element : pathElements) {
                if (element.isEmpty()) continue;
                currentPath = currentPath.isEmpty() ? element : currentPath + "\\" + element;
                if (!diskShare.folderExists(currentPath)) {
                    diskShare.mkdir(currentPath);
                }
            }
        }
    }

    protected SMBClient createSmbClient() {
        return new SMBClient();
    }
}
