package io.eel.engine_deployments_aws_lambda.dao;

import io.eel.common.dao.WorkbookDao;
import io.eel.common.model.StorageLocation;
import io.eel.dao.WorkbookLoggerDao;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

public class S3WorkbookLoggerDaoImpl implements WorkbookLoggerDao {

    private static final Logger log = LoggerFactory.getLogger(S3WorkbookLoggerDaoImpl.class);

    private boolean debugModeEnabled = false;

    private WorkbookDao workbookDao;

    private String bucketName;

    private S3WorkbookLoggerDaoImpl(boolean debugModeEnabled) {
        this.debugModeEnabled = debugModeEnabled;
    }

    public S3WorkbookLoggerDaoImpl(WorkbookDao workbookDao, String bucketName) {
        this.workbookDao = workbookDao;
        this.bucketName = bucketName;
    }

    @Override
    public Optional<StorageLocation> log(Workbook workbook, String workbookName) {
        if (this.debugModeEnabled) {
            log.debug("Debug mode is enabled");

            String randomSuffix = UUID.randomUUID().toString();
            String key = workbookName + randomSuffix + ".xlsx";

            log.debug("Writing workbook {} to bucket {} and key {}", workbookName, bucketName, key);

            this.workbookDao.save(workbook, bucketName, key);

            return Optional.of(new StorageLocation(bucketName, key, null, null));
        }

        log.debug("Debug mode is disabled");

        return Optional.empty();
    }

}
