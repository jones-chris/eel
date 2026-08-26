package io.eel.common;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class WorkbookValidatorTest {

    @Test
    public void createManifestIsSuccessful() throws IOException {
        Workbook workbook = new XSSFWorkbook(new FileInputStream("/home/pc/repos/eel/engine/engine-core/src/main/resources/eel.xlsx"));

        WorkbookValidator.Manifest manifest = new WorkbookValidator(workbook, "author", "name", 0, UUID.randomUUID(), "")
                .assertIsValid()
                .createManifest();

        System.out.println(manifest);
        assertNotNull(manifest);
    }

}