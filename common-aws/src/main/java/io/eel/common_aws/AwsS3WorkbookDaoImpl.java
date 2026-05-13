package io.eel.common_aws;

import io.eel.common.dao.WorkbookDao;
import io.eel.common.model.WorkbookProxy;
import org.apache.poi.ss.usermodel.Workbook;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.*;
import java.util.Optional;
import java.util.logging.Logger;

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

    @Override
    public void save(Workbook workbook, String bucket, String key) {
        log.info("Saving workbook to bucket " + bucket + " and key " + key);

        File tempFile = null;
        try {
            tempFile = File.createTempFile("workbook", ".xlsx");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                workbook.write(fos);
            }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            this.s3Client.putObject(putObjectRequest, RequestBody.fromFile(tempFile));
        } catch (IOException e) {
            log.severe(e.getMessage());
            e.printStackTrace();  // todo: fix this.

            throw new RuntimeException(e);
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

}
