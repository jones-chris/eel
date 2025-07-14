package io.eel.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.eel.common.EelPackager;
import io.eel.common.WorkbookValidator;
import org.apache.poi.ss.formula.eval.FunctionEval;
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
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class StreamLambdaHandler implements RequestHandler<Map<String, String>, Object> {

    private static final String EEL_XLSX_FILE_NAME = "eel.xlsx";

    /**
     * The platform-dependent temp directory path.
     */
    private static final String tmpPath = System.getProperty("java.io.tmpdir");

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Logger log = Logger.getLogger(StreamLambdaHandler.class.getName());

    private static final S3Client s3Client = S3Client.builder().build();

    public static void main(String[] args) {
        Collection<String> supportedFunctions = FunctionEval.getSupportedFunctionNames();
        log.info("Supported Functions:  \n");
        supportedFunctions.forEach(log::info);

        Collection<String> unsupportedFunctions = FunctionEval.getNotSupportedFunctionNames();
        log.info("Unsupported Functions:  \n");
        unsupportedFunctions.forEach(log::info);

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
            log.info("Event: " + event);
            log.info("Context: " +  context);

            final String originalJarBucketName = System.getenv("ORIGINAL_JAR_BUCKET_NAME");
            final String originalJarObjectKey = System.getenv("ORIGINAL_JAR_OBJECT_KEY");

            final String excelBucketName = event.get("bucketName");
            final String excelObjectKey = event.get("objectKey");
            final String author = event.get("author");
            final String name = event.get("name");
            final Integer version = Integer.parseInt(event.get("version"));

            // Download original EEL JAR from S3 and write it to a tmp file.
            // todo:  Consider downloading this only once and re-using it for future invocations.
            GetObjectRequest jarRequest = GetObjectRequest.builder()
                    .bucket(originalJarBucketName)
                    .key(originalJarObjectKey)
                    .build();
            ResponseBytes<GetObjectResponse> originalJarS3Object = s3Client.getObject(jarRequest, ResponseTransformer.toBytes());
            InputStream originalJarInputStream = originalJarS3Object.asInputStream();
            File jarTmpFile = new File(tmpPath + "/NEW_EEL.jar");
            Files.copy(originalJarInputStream, jarTmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            originalJarInputStream.close();

            log.info("Wrote EEL.jar to " + jarTmpFile.toPath());

            // Download Excel file from S3 and write it to a tmp file.
            GetObjectRequest excelRequest = GetObjectRequest.builder()
                    .bucket(excelBucketName)
                    .key(excelObjectKey)
                    .build();
            ResponseBytes<GetObjectResponse> excelS3Object = s3Client.getObject(excelRequest, ResponseTransformer.toBytes());
            InputStream excelInputStream = excelS3Object.asInputStream();
            File tmpFile = new File(tmpPath + "/hello.xlsx");
            Files.copy(excelInputStream, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            excelInputStream.close();

            // Create manifest and write it to a tmp file.
            InputStream excelInputStream2 = excelS3Object.asInputStream();
            WorkbookValidator.Manifest manifest = EelPackager.createManifest(excelInputStream2, author, name, version);
            excelInputStream2.close();
            final String manifestJson = gson.toJson(manifest);
            log.info(manifestJson);

            File tmpManifestFile = new File(tmpPath + "/eel_manifest.json");
            Files.write(tmpManifestFile.toPath(), manifestJson.getBytes());
            log.info("Wrote manifest JSON to " + tmpManifestFile.getAbsolutePath());

            // Add manifest and excel workbook to JAR.
            final String jarTmpFilePath = jarTmpFile.toPath().toAbsolutePath().toString();
            Process manifestCopyProcess = Runtime.getRuntime()
                    .exec(
                        new String[] {
                                // Add manifest to JAR's resources.
                                "jar", "uf", jarTmpFilePath, "-C", tmpPath, tmpManifestFile.getName()
                        }
                    );
            awaitSubProcess(manifestCopyProcess);

            Process excelCopyProcess = Runtime.getRuntime()
                    .exec(
                            new String[] {
                                // Add Excel file to JAR's resources.
                                "jar", "uf", jarTmpFilePath, "-C", tmpPath, tmpFile.getName()
                            }
                    );
            awaitSubProcess(excelCopyProcess);

            // Save JAR to S3 and return S3 path.
            log.info("Writing file " + jarTmpFilePath + " to S3 bucket");
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket("eel-test")
                    .key(jarTmpFile.getName())
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

    private static void awaitSubProcess(Process process) throws InterruptedException {
        int maxCounts = 3;
        int counts = 0;
        while (process.isAlive()) {
            if (counts == maxCounts) {
                throw new RuntimeException("Manifest copy process timed out");
            }

            counts++;
            Thread.sleep(Duration.ofSeconds(15));
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            final String errorOutput = getProcessErrorOutput(process);
            throw new RuntimeException(
                    String.format("Manifest Copy Process exited with status code %d.  Here is the error:  %s", exitCode, errorOutput)
            );
        }
    }

}
