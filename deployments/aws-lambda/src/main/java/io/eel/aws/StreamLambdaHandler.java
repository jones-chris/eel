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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StreamLambdaHandler implements RequestHandler<Map<String, String>, Object> {

    private static final String EEL_XLSX_FILE_NAME = "eel.xlsx";

    private static final S3Client s3Client = S3Client.builder().build();

    public static void main(String[] args) {
        new StreamLambdaHandler().handleRequest(
                Map.of(
                        "bucketName", "eel-test",
                        "objectKey", "transformation.xlsx",
                        "author", "pc",
                        "name", "myTransformation",
                        "version", "0"
                ),
                null
        );
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
            File tmpFile = File.createTempFile("hello", ".xlsx");
//            Files.copy(excelInputStream, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Create manifest.
            Path manifestPath = EelPackager.pack(excelInputStream, author, name, version);

            // Add manifest and excel workbook to JAR.
            final String jarTmpFilePath = jarTmpFile.toPath().toAbsolutePath().toString();
            Process manifestCopyProcess = Runtime.getRuntime()
                    .exec(
                        new String[] {
                                // Add manifest to JAR's resources.
                                "jar", "uf", jarTmpFilePath, manifestPath.toString()
                        }
                    );
            int maxCounts = 3;
            int counts = 0;
            while (manifestCopyProcess.isAlive()) {
                if (counts == maxCounts) {
                    throw new RuntimeException("Manifest copy process timed out");
                }

                counts++;
                Thread.sleep(Duration.ofSeconds(15));
            }
            int exitCode = manifestCopyProcess.exitValue();
            if (exitCode != 0) {
                final String errorOutput = getProcessErrorOutput(manifestCopyProcess);
                throw new RuntimeException(
                        String.format("Manifest Copy Process exited with status code %d.  Here is the error:  %s", exitCode, errorOutput)
                );
            }

            Process excelCopyProcess = Runtime.getRuntime()
                    .exec(
                            new String[] {
                                // Add Excel file to JAR's resources.
                                "jar", "uf", jarTmpFilePath, tmpFile.toString()
//                                String.format("jar uf %s %s", jarTmpFilePath, tmpFile.toPath().toAbsolutePath())
                            }
                    );
            int exitCode1 = excelCopyProcess.exitValue();
            if (exitCode1 != 0) {
                final String errorOutput = getProcessErrorOutput(excelCopyProcess);
                throw new RuntimeException(
                        String.format("Excel copy process exited with status code %d. Here is the error %s", exitCode1, errorOutput)
                );
            }

            // Save JAR to S3 and return S3 path.
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket("")
                    .key("")
                    .build();
            s3Client.putObject(putRequest, jarTmpFile.toPath());
        } catch (Throwable t) {
            // todo:  Add logic here.
            t.printStackTrace();
            return "failure";
        }

        // todo:  Change this.
        return "success";
    }

    private static String getProcessErrorOutput(Process process) {
        try (BufferedReader reader = process.errorReader()) {
            return reader.lines()
                    .collect(Collectors.joining());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
