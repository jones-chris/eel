package io.eel.manifest_generator_core.service;

import io.eel.common.EelPackager;
import io.eel.common.model.Flow;
import io.eel.common.model.TransformationExtractionType;
import io.eel.common_aws.util.S3Utils;
import io.eel.manifest_generator_core.model.ArtifactBuild;
import io.eel.manifest_generator_core.dao.ArtifactDao;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public class ArtifactServiceImpl implements ArtifactService {

    private static final String ORIGINAL_EEL_JAR_BUCKET;

    private static final String ORIGINAL_EEL_JAR_KEY;

//    private static final String EEL_TRANSFORMATIONS_BUCKET_NAME;

    private static final String JAR_BUCKET_NAME;

    private S3Client s3Client;

    private S3Utils s3Utils;

    private ArtifactDao artifactDao;

    static {
        ORIGINAL_EEL_JAR_BUCKET = Optional.ofNullable(System.getenv("ORIGINAL_EEL_JAR_BUCKET"))
                .orElseThrow(() -> new RuntimeException("Environment variable ORIGINAL_EEL_JAR_BUCKET is not set"));

        ORIGINAL_EEL_JAR_KEY = Optional.ofNullable(System.getenv("ORIGINAL_EEL_JAR_KEY"))
                .orElseThrow(() -> new RuntimeException("Environment variable ORIGINAL_EEL_JAR_KEY is not set"));

//        EEL_TRANSFORMATIONS_BUCKET_NAME = Optional.ofNullable(System.getenv("EEL_TRANSFORMATIONS_BUCKET_NAME"))
//                .orElseThrow(() -> new RuntimeException("Environment variable EEL_TRANSFORMATIONS_BUCKET_NAME is not set"));

        JAR_BUCKET_NAME = Optional.ofNullable(System.getenv("JAR_BUCKET_NAME"))
                .orElseThrow(() -> new RuntimeException("Environment variable JAR_BUCKET_NAME is not set"));
    }

    private ArtifactServiceImpl() {}

    public ArtifactServiceImpl(S3Client s3Client, ArtifactDao artifactDao) {
        this.s3Client = s3Client;
        this.s3Utils = new S3Utils(s3Client);
        this.artifactDao = artifactDao;
    }

    @Override
    public String getPresignedUrl(String flowCanonicalId) {
        try (S3Presigner preSigner = S3Presigner.create()) {
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .bucket(JAR_BUCKET_NAME)
                    .key(TransformationExtractionType.ARTIFACT.getPrefix() + flowCanonicalId)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(1))
                    .getObjectRequest(objectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = preSigner.presignGetObject(presignRequest);

            String url = presignedRequest.url().toString();
            System.out.println("Presigned URL to upload a file to: " + url);
            System.out.println("HTTP method: " + presignedRequest.httpRequest().method());
            System.out.println("Presigned external URL to a upload file to: " + presignedRequest.url().toExternalForm());

            return url;
        }
    }

    @Override
    public void buildJar(String bucket, String key) {
        String canonicalId = null;
        ArtifactBuild artifactBuild = null;

        try {
            InputStream originalJarInputStream = this.s3Utils.getS3ObjectAsInputStream(ORIGINAL_EEL_JAR_BUCKET, ORIGINAL_EEL_JAR_KEY);
            InputStream excelInputStream = this.s3Utils.getS3ObjectAsInputStream(bucket, key);
//            InputStream excelInputStream = this.getClass().getResourceAsStream("/eel.xlsx");

            final File eelJar = EelPackager.build(originalJarInputStream, excelInputStream);

            String strippedKey = key.replace(TransformationExtractionType.ARTIFACT.getPrefix(), "");
            canonicalId = Flow.Utils.getCanonicalId(UUID.fromString(strippedKey), 0);

            this.uploadJarToS3(eelJar, canonicalId);

            // Write entry to DDB with error (if failure) or link (if successful)
            artifactBuild = new ArtifactBuild(canonicalId, JAR_BUCKET_NAME, canonicalId, null);
        } catch (Throwable t) {
            // todo:  fix this logging later.
            t.printStackTrace();
            artifactBuild = new ArtifactBuild(canonicalId, JAR_BUCKET_NAME, canonicalId, t.getMessage());
        } finally {
            this.artifactDao.saveArtifactBuild(artifactBuild);
        }
    }

    @Override
    public Optional<ArtifactBuild> getArtifactBuild(String flowCanonicalId) {
        return this.artifactDao.getArtifactBuild(flowCanonicalId);
    }

    // todo: This should be in a DAO class.
    private void uploadJarToS3(File jarFile, String flowId) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(JAR_BUCKET_NAME)
                .key(flowId)
                .contentType("application/java-archive")
                .build();

        this.s3Client.putObject(putObjectRequest, jarFile.toPath());
    }

}
