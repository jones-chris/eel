package io.eel.dao;

import io.eel.common.model.StorageLocation;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class TempFileWorkbookLoggerDaoImpl implements WorkbookLoggerDao {

    private static final Logger log = LoggerFactory.getLogger(TempFileWorkbookLoggerDaoImpl.class);

    @Override
    public Optional<StorageLocation> log(Workbook workbook, String workbookName) {
        try {
            Path tempDirectoryPath = Files.createTempDirectory(workbookName);
            Path tempFilePath = Files.createTempFile(tempDirectoryPath, null, ".xlsx");

            log.debug("Writing log workbook to {}", tempFilePath.toFile().getAbsolutePath());

            try (OutputStream outputStream = new FileOutputStream(tempFilePath.toFile())) {
                workbook.write(outputStream);
            }

            return Optional.of(
                    new StorageLocation(tempDirectoryPath.toString(), tempFilePath.toString(), null, null)
            );
        } catch (Throwable t) {
            log.error("", t);

            return Optional.empty();
        }
    }

}
