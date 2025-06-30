package io.eel.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.eel.packager.EelPackager;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class StreamLambdaHandler implements RequestHandler<Map<String, String>, Object> {

    private static final String EEL_XLSX_FILE_NAME = "eel.xlsx";

    private static final S3Client s3Client = S3Client.builder().build();

    public static void main(String[] args) {
        new StreamLambdaHandler().handleRequest(Map.of(), null);
    }

    @Override
    public Object handleRequest(Map<String, String> event, Context context) {
        try {
            final String originalJarBucketName = System.getenv("ORIGINAL_JAR_BUCKET_NAME");
            final String originalJarObjectKey = System.getenv("ORIGINAL_JAR_OBJECT_KEY");

            final String excelBucketName = event.get("bucketName");
            final String excelObjectKey = event.get("objectKey");
            final String author = event.get("author");
            final String name = event.get("name");
            final Integer version = Integer.parseInt(event.get("version"));

            // Download original EEL JAR from S3.
            // todo:  Consider downloading this only once and re-using it for future invocations.
            GetObjectRequest jarRequest = GetObjectRequest.builder()
                    .bucket(originalJarBucketName)
                    .key(originalJarObjectKey)
                    .build();
            ResponseBytes<GetObjectResponse> originalJarS3Object = s3Client.getObject(jarRequest, ResponseTransformer.toBytes());
            InputStream originalJarInputStream = originalJarS3Object.asInputStream();
            File jarTmpFile = File.createTempFile("EEL", ".jar");
            Files.copy(originalJarInputStream, jarTmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Download Excel file from S3.
            GetObjectRequest excelRequest = GetObjectRequest.builder()
                    .bucket(excelBucketName)
                    .key(excelObjectKey)
                    .build();
            ResponseBytes<GetObjectResponse> excelS3Object = s3Client.getObject(excelRequest, ResponseTransformer.toBytes());
            InputStream excelInputStream = excelS3Object.asInputStream();
            File tmpFile = File.createTempFile("eel", ".xlsx");
            Files.copy(excelInputStream, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Create manifest.
            Path manifestPath = EelPackager.pack(excelInputStream, author, name, version);

            // Add manifest and Excel to JAR.
            final String jarTmpFilePath = jarTmpFile.toPath().toAbsolutePath().toString();
            Runtime.getRuntime()
                    .exec(
                        new String[]{
                                // Add manifest to JAR's resources.
                                "jar", "uf", jarTmpFilePath, manifestPath.toAbsolutePath().toString(), "&&",
                                // Add Excel file to JAR's resources.
                                "jar", "uf", jarTmpFilePath, manifestPath.toAbsolutePath().toString()
                        }
                    );

            // Save JAR to S3 and return S3 path.
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket("")
                    .key("")
                    .build();
            s3Client.putObject(putRequest, jarTmpFile.toPath());
        } catch (Throwable t) {
            // todo:  Add logic here.
            return "failure";
        }

        // todo:  Change this.
        return "success";
    }

}
