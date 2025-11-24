package io.eel.engine_deployments_aws_lambda.dao;

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

public class S3WorkbookInputsDaoImpl implements WorkbookInputsDao {

    private static final Logger log = Logger.getLogger(S3WorkbookInputsDaoImpl.class.getName());

    private S3Client s3Client;

    private S3WorkbookInputsDaoImpl() {}

    public S3WorkbookInputsDaoImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public Optional<ZipInputStream> getWorkbookInputs(String bucket, String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            log.info("Getting workbook at bucket " + bucket + " and key " + key);

            ResponseBytes<GetObjectResponse> inputObject = this.s3Client.getObject(request, ResponseTransformer.toBytes());

            try (InputStream inputStream = inputObject.asInputStream()) {
                return Optional.of(new ZipInputStream(inputStream));
            }
        } catch (Throwable t) {
            log.severe(t.getMessage());

            throw new RuntimeException(t);
        }
    }

}
