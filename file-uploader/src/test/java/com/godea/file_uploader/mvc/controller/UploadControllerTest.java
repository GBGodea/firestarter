package com.godea.file_uploader.mvc.controller;

import com.godea.file_uploader.controller.UploadController;
import com.godea.file_uploader.dto.UploadResponse;
import com.godea.file_uploader.service.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UploadControllerTest {

    @Mock
    private FileUploadService service;

    @InjectMocks
    private UploadController controller;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void upload_endpoint_ok() throws Exception {
        var file = new MockMultipartFile("file","test.xls","application/octet-stream",new byte[]{1});
        when(service.processFile(any()))
                .thenReturn(UploadResponse.builder().status("success").fileHash("h").build());

        mvc.perform(multipart("/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.fileHash").value("h"));

        verify(service).processFile(any());
    }

    @Test
    void upload_endpoint_error() throws Exception {
        var file = new MockMultipartFile("file","test.xls","application/octet-stream",new byte[]{1});
        when(service.processFile(any())).thenThrow(new RuntimeException("err"));

        mvc.perform(multipart("/upload").file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorType").value("internal"));
    }
}
