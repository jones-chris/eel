package io.eel.manifest_api_aws_lambda.dao;

import io.eel.manifest_generator_core.dao.WorkbookDao;
import io.eel.model.proxy.WorkbookProxy;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.InputStream;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

public class AwsS3WorkbookDaoImpl implements WorkbookDao {

    private static final Logger log = Logger.getLogger(AwsS3WorkbookDaoImpl.class.getName());

    private S3Client s3Client;

    private AwsS3WorkbookDaoImpl() {}

    public AwsS3WorkbookDaoImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public Optional<WorkbookProxy> getWorkbook(String bucket, String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        log.info("Getting workbook at bucket " + bucket + " and key " + key);
        ResponseBytes<GetObjectResponse> workbookS3Object = this.s3Client.getObject(request, ResponseTransformer.toBytes());

        try (InputStream inputStream = workbookS3Object.asInputStream()) {
            return Optional.of(new WorkbookProxy(inputStream));
        } catch (Throwable t) {
            log.severe(t.getMessage());

            return Optional.empty();
        }
    }
}
