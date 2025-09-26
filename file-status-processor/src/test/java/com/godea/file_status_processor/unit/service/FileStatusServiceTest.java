package com.godea.file_status_processor.unit.service;

import com.godea.file_status_processor.dto.StatusEvent;
import com.godea.file_status_processor.model.FileStatus;
import com.godea.file_status_processor.repository.FileStatusRepository;
import com.godea.file_status_processor.service.FileStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileStatusServiceTest {

    @InjectMocks
    private FileStatusService service;

    @Mock
    private FileStatusRepository repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void whenNewStatusEvent_thenCreatesNewFileStatus() {
        StatusEvent event = new StatusEvent();
        event.setEvent("accepted");
        event.setFileHash("hash123");
        event.setFilename("test.xlsx");
        event.setMessage("File accepted");
        event.setTimestamp(Instant.now());

        when(repository.findById("hash123")).thenReturn(Optional.empty());
        when(repository.save(any(FileStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.handleStatusEvent(event);

        ArgumentCaptor<FileStatus> captor = ArgumentCaptor.forClass(FileStatus.class);
        verify(repository).save(captor.capture());

        FileStatus saved = captor.getValue();
        assertThat(saved.getFileHash()).isEqualTo("hash123");
        assertThat(saved.getStatus()).isEqualTo("accepted");
        assertThat(saved.getFilename()).isEqualTo("test.xlsx");
        assertThat(saved.getMessage()).isEqualTo("File accepted");
        assertThat(saved.getTimestamp()).isEqualTo(event.getTimestamp().toEpochMilli());
    }

    @Test
    void whenExistingStatusEvent_thenUpdatesExistingFileStatus() {
        StatusEvent event = new StatusEvent();
        event.setEvent("validation_success");
        event.setFileHash("hash456");
        event.setFilename("test.xlsx");
        event.setMessage("Validation passed");
        event.setTimestamp(Instant.now());

        FileStatus existing = FileStatus.builder()
                .fileHash("hash456")
                .status("accepted")
                .filename("test.xlsx")
                .message("File accepted")
                .timestamp(System.currentTimeMillis() - 1000)
                .build();

        when(repository.findById("hash456")).thenReturn(Optional.of(existing));
        when(repository.save(any(FileStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handleStatusEvent(event);

        ArgumentCaptor<FileStatus> captor = ArgumentCaptor.forClass(FileStatus.class);
        verify(repository).save(captor.capture());

        FileStatus updated = captor.getValue();
        assertThat(updated.getFileHash()).isEqualTo("hash456");
        assertThat(updated.getStatus()).isEqualTo("validation_success");
        assertThat(updated.getMessage()).isEqualTo("Validation passed");
        assertThat(updated.getTimestamp()).isEqualTo(event.getTimestamp().toEpochMilli());
        assertThat(updated.getFilename()).isEqualTo("test.xlsx");
    }

    @Test
    void whenGetStatus_thenReturnsFileStatus() {
        FileStatus fileStatus = FileStatus.builder()
                .fileHash("hash789")
                .status("validation_success")
                .filename("test.xlsx")
                .message("All good")
                .timestamp(System.currentTimeMillis())
                .build();

        when(repository.findById("hash789")).thenReturn(Optional.of(fileStatus));
        FileStatus result = service.getStatus("hash789");

        assertThat(result).isNotNull();
        assertThat(result.getFileHash()).isEqualTo("hash789");
        assertThat(result.getStatus()).isEqualTo("validation_success");
    }

    @Test
    void whenGetStatusNotFound_thenReturnsNull() {
        when(repository.findById("nonexistent")).thenReturn(Optional.empty());
        FileStatus result = service.getStatus("nonexistent");
        assertThat(result).isNull();
    }
}
