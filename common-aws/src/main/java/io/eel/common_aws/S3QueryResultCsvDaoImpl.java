package io.eel.common_aws;

import io.eel.common.dao.QueryResultCsvDao;
import io.eel.common.model.StorageLocation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.logging.Logger;

public class S3QueryResultCsvDaoImpl implements QueryResultCsvDao {

    private final static Logger log = Logger.getLogger(S3QueryResultCsvDaoImpl.class.getName());

    private S3Client s3Client;

    private S3QueryResultCsvDaoImpl() {}

    public S3QueryResultCsvDaoImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public StorageLocation save(byte[] queryResultCsvBytes, StorageLocation storageLocation) {
        log.info("Saving query result CSV bytes to bucket " + storageLocation.bucket() + " and key " + storageLocation.key());

        final PutObjectRequest request = PutObjectRequest.builder()
                .bucket(storageLocation.bucket())
                .key(storageLocation.key())
                .contentType("text/csv")
                .build();

        final RequestBody requestBody = RequestBody.fromBytes(queryResultCsvBytes);

        this.s3Client.putObject(request, requestBody);

        return storageLocation;
    }

}
