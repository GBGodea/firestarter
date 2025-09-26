package com.godea.file_processor.service;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class FileValidationService {
    public void validate(byte[] fileBytes) {
        try(InputStream in = new ByteArrayInputStream(fileBytes)) {
            Workbook workbook = WorkbookFactory.create(in);
            Sheet sheet = workbook.getSheetAt(0);
            for(int rowInd = 0; rowInd < 2; rowInd++) {
                Row row = sheet.getRow(rowInd);
                if(row == null) {
                    throw new IllegalArgumentException("Row " + rowInd + " is null");
                }
                for(int collInd = 0; collInd < 3; collInd++) {
                    Cell cell = row.getCell(collInd, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if(cell == null || cell.getCellType() == CellType.BLANK) {
                        throw new IllegalArgumentException(
                                String.format("Cell [%d, %d] is blank", rowInd, collInd)
                        );
                    }
                }
            }
        } catch(IllegalArgumentException e) {
            throw e;
        } catch(InvalidFormatException e) {
            throw new RuntimeException("Unsupported Excel format", e);
        } catch(IOException e) {
            throw new RuntimeException("Failed to read Excel file", e);
        }
    }
}
