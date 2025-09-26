package com.godea.file_status_processor.integration;

import com.godea.file_status_processor.controller.FileStatusController;
import com.godea.file_status_processor.model.FileStatus;
import com.godea.file_status_processor.service.FileStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileStatusController.class)
class FileStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStatusService statusService;

    @Test
    void whenGetStatus_thenReturnsFileStatus() throws Exception {
        FileStatus fileStatus = FileStatus.builder()
                .fileHash("hash123")
                .status("validation_success")
                .filename("test.xlsx")
                .message("All good")
                .timestamp(System.currentTimeMillis())
                .build();

        when(statusService.getStatus("hash123")).thenReturn(fileStatus);

        mockMvc.perform(get("/status/{filehash}", "hash123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileHash").value("hash123"))
                .andExpect(jsonPath("$.status").value("validation_success"))
                .andExpect(jsonPath("$.filename").value("test.xlsx"))
                .andExpect(jsonPath("$.message").value("All good"));
    }

    @Test
    void whenGetStatusNotFound_thenReturnsNull() throws Exception {
        when(statusService.getStatus("nonexistent")).thenReturn(null);
        mockMvc.perform(get("/status/{fileHash}", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }
}
