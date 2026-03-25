package com.github.xjesusx0.cloudbox.infrastructure.ftp;

import com.github.xjesusx0.cloudbox.application.dtos.FileMetadata;
import com.github.xjesusx0.cloudbox.core.exceptions.FileListException;
import com.github.xjesusx0.cloudbox.core.exceptions.FileUploadException;
import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FtpStorageStrategyTest {

    @Mock
    private FTPSClient ftpClient;

    private FtpStorageStrategy strategy;

    @BeforeEach
    void setUp() {
        // Create spy to mock createFtpClient method
        strategy = spy(new FtpStorageStrategy("localhost", 21, "user", "pass"));
        lenient().doReturn(ftpClient).when(strategy).createFtpClient();
    }

    @Test
    void getProtocol_shouldReturnFtp() {
        assertEquals(StorageProtocol.FTP, strategy.getProtocol());
    }

    @Test
    void save_shouldUploadFileSuccessfully() throws IOException {
        String userId = "user123";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

        when(ftpClient.login("user", "pass")).thenReturn(true);
        when(ftpClient.printWorkingDirectory()).thenReturn("/");
        when(ftpClient.changeWorkingDirectory(anyString())).thenReturn(true); 
        when(ftpClient.storeFile(eq(userId + "/test.txt"), any(InputStream.class))).thenReturn(true);
        when(ftpClient.isConnected()).thenReturn(true);

        assertDoesNotThrow(() -> strategy.save(file, userId));

        verify(ftpClient).connect("localhost", 21);
        verify(ftpClient).login("user", "pass");
        verify(ftpClient).enterLocalPassiveMode();
        verify(ftpClient).setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
        verify(ftpClient).execPBSZ(0);
        verify(ftpClient).execPROT("P");
        verify(ftpClient).storeFile(eq(userId + "/test.txt"), any(InputStream.class));
        verify(ftpClient).logout();
        verify(ftpClient).disconnect();
    }

    @Test
    void save_shouldThrowExceptionWhenLoginFails() throws IOException {
        String userId = "user123";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

        when(ftpClient.login("user", "pass")).thenReturn(false);

        assertThrows(FileUploadException.class, () -> strategy.save(file, userId));
        
        verify(ftpClient, never()).storeFile(anyString(), any());
    }

    @Test
    void save_shouldThrowExceptionWhenUploadFails() throws IOException {
        String userId = "user123";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

        when(ftpClient.login("user", "pass")).thenReturn(true);
        when(ftpClient.printWorkingDirectory()).thenReturn("/");
        when(ftpClient.changeWorkingDirectory(anyString())).thenReturn(true);
        when(ftpClient.storeFile(eq(userId + "/test.txt"), any(InputStream.class))).thenReturn(false);

        assertThrows(FileUploadException.class, () -> strategy.save(file, userId));
    }

    @Test
    void listFiles_shouldReturnListOfFiles() throws IOException {
        String userId = "user123";

        FTPFile mockFile1 = new FTPFile();
        mockFile1.setName("doc1.pdf");
        mockFile1.setSize(1024);
        mockFile1.setType(FTPFile.FILE_TYPE);
        mockFile1.setTimestamp(Calendar.getInstance());

        FTPFile mockFile2 = new FTPFile();
        mockFile2.setName("folder1");
        mockFile2.setSize(0);
        mockFile2.setType(FTPFile.DIRECTORY_TYPE);
        mockFile2.setTimestamp(Calendar.getInstance());

        FTPFile[] files = {mockFile1, mockFile2};

        when(ftpClient.login("user", "pass")).thenReturn(true);
        when(ftpClient.changeWorkingDirectory(userId)).thenReturn(true);
        when(ftpClient.listFiles()).thenReturn(files);
        when(ftpClient.isConnected()).thenReturn(true);

        List<FileMetadata> result = strategy.listFiles(userId);

        assertEquals(2, result.size());
        assertEquals("doc1.pdf", result.get(0).getName());
        assertEquals("user123/doc1.pdf", result.get(0).getPath());
        assertEquals(1024, result.get(0).getSize());
        assertFalse(result.get(0).isDirectory());

        assertEquals("folder1", result.get(1).getName());
        assertEquals("user123/folder1", result.get(1).getPath());
        assertTrue(result.get(1).isDirectory());
        
        verify(ftpClient).connect("localhost", 21);
        verify(ftpClient).login("user", "pass");
        verify(ftpClient).execPBSZ(0);
        verify(ftpClient).execPROT("P");
        verify(ftpClient).enterLocalPassiveMode();
        verify(ftpClient).logout();
        verify(ftpClient).disconnect();
    }

    @Test
    void listFiles_shouldReturnEmptyListIfDirectoryDoesNotExist() throws IOException {
        String userId = "user123";

        when(ftpClient.login("user", "pass")).thenReturn(true);
        when(ftpClient.changeWorkingDirectory(userId)).thenReturn(false);

        List<FileMetadata> result = strategy.listFiles(userId);

        assertTrue(result.isEmpty());
        verify(ftpClient, never()).listFiles();
    }

    @Test
    void listFiles_shouldThrowExceptionWhenLoginFails() throws IOException {
        String userId = "user123";

        when(ftpClient.login("user", "pass")).thenReturn(false);

        assertThrows(FileListException.class, () -> strategy.listFiles(userId));
        
        verify(ftpClient, never()).listFiles();
    }

    @Test
    void getUsedSpace_shouldReturnZeroWhenLoginFails() throws IOException {
        String userId = "user123";

        when(ftpClient.login("user", "pass")).thenReturn(false);

        long usedSpace = strategy.getUsedSpace(userId);

        assertEquals(0L, usedSpace);
        verify(ftpClient, never()).listFiles();
    }

    @Test
    void getUsedSpace_shouldCalculateSpaceRecursively() throws IOException {
        String userId = "user123";

        FTPFile rootFile = new FTPFile();
        rootFile.setName("file1.txt");
        rootFile.setSize(100);
        rootFile.setType(FTPFile.FILE_TYPE);
        
        FTPFile folder = new FTPFile();
        folder.setName("subfolder");
        folder.setType(FTPFile.DIRECTORY_TYPE);
        
        FTPFile subFile = new FTPFile();
        subFile.setName("file2.txt");
        subFile.setSize(50);
        subFile.setType(FTPFile.FILE_TYPE);
        
        FTPFile dotFile = new FTPFile();
        dotFile.setName(".");
        dotFile.setType(FTPFile.DIRECTORY_TYPE);

        when(ftpClient.login("user", "pass")).thenReturn(true);
        when(ftpClient.changeWorkingDirectory(userId)).thenReturn(true);
        
        // Mock root directory listing
        when(ftpClient.listFiles()).thenReturn(new FTPFile[]{rootFile, folder, dotFile});
        
        // Mock subfolder listing
        when(ftpClient.listFiles("subfolder")).thenReturn(new FTPFile[]{subFile});

        long usedSpace = strategy.getUsedSpace(userId);

        assertEquals(150L, usedSpace); // 100 + 50
    }
}
