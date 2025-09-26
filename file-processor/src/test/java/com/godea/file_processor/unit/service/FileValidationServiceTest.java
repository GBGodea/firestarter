package com.godea.file_processor.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class FileValidationServiceTest {

    private final FileValidationService service = new FileValidationService();

    private byte[] createWorkbook(boolean blankCell) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("value");
            if (!blankCell) {
                row.createCell(1).setCellValue("value");
            }
            row.createCell(2).setCellValue("value");
            Row row2 = sheet.createRow(1);
            row2.createCell(0).setCellValue("x");
            row2.createCell(1).setCellValue("y");
            row2.createCell(2).setCellValue("z");

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                wb.write(out);
                return out.toByteArray();
            }
        }
    }

    @Test
    void validWorkbook_passesValidation() throws Exception {
        byte[] good = createWorkbook(false);
        service.validate(good);
    }

    @Test
    void blankCell_throwsIllegalArgumentException() throws Exception {
        byte[] bad = createWorkbook(true);
        assertThatThrownBy(() -> service.validate(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cell [0, 1] is blank");
    }
}
