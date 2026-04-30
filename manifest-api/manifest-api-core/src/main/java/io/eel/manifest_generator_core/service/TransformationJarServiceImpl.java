package io.eel.manifest_generator_core.service;

import io.eel.common.EelPackager;
import io.eel.common_aws.util.S3Utils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;

public class TransformationJarServiceImpl implements TransformationJarService {

    private static final String ORIGINAL_EEL_JAR_BUCKET;

    private static final String ORIGINAL_EEL_JAR_KEY;

    private static final String EEL_TRANSFORMATIONS_BUCKET_NAME;

    private static final String JAR_BUCKET_NAME;

    private S3Client s3Client;

    private S3Utils s3Utils;

    static {
        ORIGINAL_EEL_JAR_BUCKET = Optional.ofNullable(System.getenv("ORIGINAL_EEL_JAR_BUCKET"))
                .orElseThrow(() -> new RuntimeException("Environment variable ORIGINAL_EEL_JAR_BUCKET is not set"));

        ORIGINAL_EEL_JAR_KEY = Optional.ofNullable(System.getenv("ORIGINAL_EEL_JAR_KEY"))
                .orElseThrow(() -> new RuntimeException("Environment variable ORIGINAL_EEL_JAR_KEY is not set"));

        EEL_TRANSFORMATIONS_BUCKET_NAME = Optional.ofNullable(System.getenv("EEL_TRANSFORMATIONS_BUCKET_NAME"))
                .orElseThrow(() -> new RuntimeException("Environment variable EEL_TRANSFORMATIONS_BUCKET_NAME is not set"));

        JAR_BUCKET_NAME = Optional.ofNullable(System.getenv("JAR_BUCKET_NAME"))
                .orElseThrow(() -> new RuntimeException("Environment variable JAR_BUCKET_NAME is not set"));
    }

    private TransformationJarServiceImpl() {}

    public TransformationJarServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
        this.s3Utils = new S3Utils(s3Client);
    }

    @Override
    public String buildJar(String flowCanonicalId) {
        InputStream originalJarInputStream = this.s3Utils.getS3ObjectAsInputStream(ORIGINAL_EEL_JAR_BUCKET, ORIGINAL_EEL_JAR_KEY);
        InputStream excelInputStream = this.s3Utils.getS3ObjectAsInputStream(EEL_TRANSFORMATIONS_BUCKET_NAME, flowCanonicalId);

        final File eelJar = EelPackager.build(originalJarInputStream, excelInputStream);

        this.uploadJarToS3(eelJar, flowCanonicalId);

        return this.generatePresignedUrl(flowCanonicalId);
    }

    private void uploadJarToS3(File jarFile, String flowId) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(JAR_BUCKET_NAME)
                .key(flowId)
                .contentType("application/java-archive")
                .build();

        this.s3Client.putObject(putObjectRequest, jarFile.toPath());
    }

    private String generatePresignedUrl(String flowId) {
        try (S3Presigner preSigner = S3Presigner.create()) {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(JAR_BUCKET_NAME)
                    .key(flowId)
                    .contentType("application/java-archive")
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(1))
                    .putObjectRequest(objectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = preSigner.presignPutObject(presignRequest);

            String url = presignedRequest.url().toString();
            System.out.println("Presigned URL to upload a file to: " + url);
            System.out.println("HTTP method: " + presignedRequest.httpRequest().method());
            System.out.println("Presigned external URL to a upload file to: " + presignedRequest.url().toExternalForm());

            return url;
        }
    }

}
