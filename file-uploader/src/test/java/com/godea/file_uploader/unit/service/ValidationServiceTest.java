package com.godea.file_uploader.unit.service;

import com.godea.file_uploader.config.FileValidationProperties;
import com.godea.file_uploader.exception.FileValidationError;
import com.godea.file_uploader.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    FileValidationProperties props;

    ValidationService service;

    @BeforeEach
    void setUp() {
        when(props.getMaxSize()).thenReturn(10L);
        when(props.getAllowedExtensions()).thenReturn(List.of("xls","xlsx"));
        service = new ValidationService(props);
    }

    @Test
    void validateFile_empty_throws() {
        MultipartFile empty = new MockMultipartFile("file", new byte[0]);
        assertThrows(FileValidationError.class, () -> service.validateFile(empty));
    }

    @Test
    void validateFile_tooLarge_throws() {
        byte[] big = new byte[20];
        MultipartFile file = new MockMultipartFile("f","test.xls","", big);
        assertThrows(FileValidationError.class, () -> service.validateFile(file));
    }

    @Test
    void validateFile_badExt_throws() {
        MultipartFile file = new MockMultipartFile("f","test.txt","", new byte[1]);
        assertThrows(FileValidationError.class, () -> service.validateFile(file));
    }

    @Test
    void validateFile_valid_passes() {
        MultipartFile file = new MockMultipartFile("f","test.xlsx","", new byte[1]);
        assertDoesNotThrow(() -> service.validateFile(file));
    }
}
