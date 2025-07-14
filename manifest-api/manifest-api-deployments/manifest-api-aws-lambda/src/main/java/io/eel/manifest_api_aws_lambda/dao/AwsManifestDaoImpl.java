package io.eel.manifest_api_aws_lambda.dao;

import io.eel.common.WorkbookValidator;
import io.eel.manifest_generator_core.dao.ManifestDao;

import java.util.UUID;

public class AwsManifestDaoImpl implements ManifestDao {

    @Override
    public WorkbookValidator.Manifest getManifest(UUID uuid) {
        return null;
    }

    @Override
    public WorkbookValidator.Manifest createManifest(WorkbookValidator.Manifest manifest) {
        return null;
    }

}
