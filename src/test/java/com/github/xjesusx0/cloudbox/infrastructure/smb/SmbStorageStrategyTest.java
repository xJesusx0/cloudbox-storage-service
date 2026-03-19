package com.github.xjesusx0.cloudbox.infrastructure.smb;

import com.github.xjesusx0.cloudbox.application.dtos.FileMetadata;
import com.github.xjesusx0.cloudbox.core.exceptions.FileUploadException;
import com.github.xjesusx0.cloudbox.core.exceptions.FileListException;
import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmbStorageStrategyTest {

    private SmbStorageStrategy strategy;

    @Mock
    private SMBClient smbClient;

    @Mock
    private Connection connection;

    @Mock
    private Session session;

    @Mock
    private DiskShare diskShare;

    @Mock
    private File smbFile;

    private static final String HOST = "localhost";
    private static final String SHARE = "share";
    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";
    private static final String DOMAIN = "domain";

    @BeforeEach
    void setUp() {
        strategy = new SmbStorageStrategy(HOST, SHARE, USERNAME, PASSWORD, DOMAIN) {
            @Override
            protected SMBClient createSmbClient() {
                return smbClient;
            }
        };
    }

    @Test
    void getProtocol_ShouldReturnSmb() {
        assertEquals(StorageProtocol.SMB, strategy.getProtocol());
    }

    @Test
    void save_SuccessfulUpload_ShouldNotThrowException() throws Exception {
        // Arrange
        MultipartFile multipartFile = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        String userId = "user123";

        when(smbClient.connect(HOST)).thenReturn(connection);
        when(connection.authenticate(any(AuthenticationContext.class))).thenReturn(session);
        when(session.connectShare(SHARE)).thenReturn(diskShare);
        
        when(diskShare.folderExists(userId)).thenReturn(true);
        when(diskShare.openFile(anyString(), any(), any(), any(), any(), any())).thenReturn(smbFile);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(smbFile.getOutputStream()).thenReturn(outputStream);

        // Act & Assert
        assertDoesNotThrow(() -> strategy.save(multipartFile, userId));
        
        verify(diskShare).openFile(eq("user123\\test.txt"), any(), any(), any(), any(), any());
        verify(smbClient).close();
    }

    @Test
    void save_FailedConnection_ShouldThrowFileUploadException() throws Exception {
        // Arrange
        MultipartFile multipartFile = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        String userId = "user123";

        when(smbClient.connect(HOST)).thenThrow(new IOException("Connection failed"));

        // Act & Assert
        FileUploadException exception = assertThrows(FileUploadException.class, () -> strategy.save(multipartFile, userId));
        assertTrue(exception.getMessage().contains("Error uploading file to SMB server"));
        verify(smbClient).close();
    }

    @Test
    void listFiles_SuccessfulListing_ShouldReturnFileMetadataList() throws Exception {
        // Arrange
        String userId = "user123";

        when(smbClient.connect(HOST)).thenReturn(connection);
        when(connection.authenticate(any(AuthenticationContext.class))).thenReturn(session);
        when(session.connectShare(SHARE)).thenReturn(diskShare);
        when(diskShare.folderExists(userId)).thenReturn(true);

        FileIdBothDirectoryInformation fileInfo1 = mock(FileIdBothDirectoryInformation.class);
        when(fileInfo1.getFileName()).thenReturn("file1.txt");
        when(fileInfo1.getEndOfFile()).thenReturn(1024L);
        when(fileInfo1.getFileAttributes()).thenReturn(0L);
        when(fileInfo1.getLastWriteTime()).thenReturn(null);

        FileIdBothDirectoryInformation dotInfo = mock(FileIdBothDirectoryInformation.class);
        when(dotInfo.getFileName()).thenReturn(".");

        when(diskShare.list(userId)).thenReturn(List.of(dotInfo, fileInfo1));

        // Act
        List<FileMetadata> files = strategy.listFiles(userId);

        // Assert
        assertEquals(1, files.size());
        assertEquals("file1.txt", files.get(0).getName());
        assertEquals(1024L, files.get(0).getSize());
        assertFalse(files.get(0).isDirectory());
        verify(smbClient).close();
    }

    @Test
    void listFiles_FailedConnection_ShouldThrowFileListException() throws Exception {
        // Arrange
        String userId = "user123";

        when(smbClient.connect(HOST)).thenThrow(new IOException("Connection failed"));

        // Act & Assert
        FileListException exception = assertThrows(FileListException.class, () -> strategy.listFiles(userId));
        assertTrue(exception.getMessage().contains("Error listing files from SMB server"));
        verify(smbClient).close();
    }
}
