package io.eel.engine_deployments_aws_lambda.dao;

import java.util.Optional;
import java.util.zip.ZipInputStream;

public interface WorkbookInputsDao {

    Optional<ZipInputStream> getWorkbookInputs(String bucket, String key);

}
