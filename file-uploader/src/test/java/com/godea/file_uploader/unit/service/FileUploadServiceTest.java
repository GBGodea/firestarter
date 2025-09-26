package com.godea.file_uploader.unit.service;

import com.godea.file_uploader.dto.UploadResponse;
import com.godea.file_uploader.exception.FileValidationError;
import com.godea.file_uploader.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @Mock
    HashService hashService;
    @Mock
    ValidationService validationService;
    @Mock
    EventPublisherService publisher;
    @Mock
    FileHashStoreService store;

    @InjectMocks
    FileUploadService uploadService;

    @Test
    void whenDuplicate_thenPublishesAlreadyUploaded() throws IOException {
        MockMultipartFile f = new MockMultipartFile("f","a.xls","", new byte[]{1});
        when(hashService.generateFileHash(f)).thenReturn("h");
        when(store.exists("h")).thenReturn(true);

        UploadResponse resp = uploadService.processFile(f);
        assertEquals("duplicate", resp.getStatus());
        verify(publisher).publishStatusEvent(argThat(dto->"already_uploaded".equals(dto.getEventType())));
    }

    @Test
    void whenValid_thenPublishesUploadAndStatus() throws IOException {
        MockMultipartFile f = new MockMultipartFile("f","a.xlsx","", new byte[]{1});
        when(hashService.generateFileHash(f)).thenReturn("h");
        when(store.exists("h")).thenReturn(false);

        UploadResponse resp = uploadService.processFile(f);
        assertEquals("success", resp.getStatus());
        verify(publisher).publishStatusEvent(argThat(dto->"validation_success".equals(dto.getEventType())));
        verify(publisher).publishUploadEvent(any());
    }

    @Test
    void whenValidationFails_thenThrowsAndPublishesError() throws IOException {
        MockMultipartFile f = new MockMultipartFile("f","a.xlsx","", new byte[]{1});
        when(hashService.generateFileHash(f)).thenReturn("h");
        when(store.exists("h")).thenReturn(false);
        doThrow(new FileValidationError("bad")).when(validationService).validateFile(f);

        assertThrows(FileValidationError.class, () -> uploadService.processFile(f));
        verify(publisher).publishStatusEvent(argThat(dto->"validation_error".equals(dto.getEventType())));
    }
}
