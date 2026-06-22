package io.eel.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class EelPackager {

    private static final Logger log = Logger.getLogger(EelPackager.class.getName());

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * An alternative entry point that exposes the {@link EelPackager#createManifest(String, String, String, Integer)} method
     * to scripts.
     *
     * @param args {@link String[]}
     */
    public static void main(String[] args) throws IOException {
        System.out.println(Arrays.toString(args));

        // Get Excel file name.
        final String excelFileName = args[0];


        final String author = System.getProperty("user.name");
        final String name = "test";
        final int version = 0;
        final UUID id = UUID.randomUUID();

        // Generate the manifest, serialize it to JSON, and write it to a file.
        WorkbookValidator.Manifest manifest = createManifest(excelFileName, author, name, version, id);

        String manifestJson = gson.toJson(manifest);

        Path tmpFilePath = File.createTempFile("eel_manifest", ".json").toPath();
        Files.write(tmpFilePath, manifestJson.getBytes());
    }

    /**
     * Validates an Excel workbook and creates the manifest from an Excel file's {@link String} path.
     *
     * @param excelFileName The file path {@link String} of the Excel workbook.
     * @param author The author of the workbook.
     * @param name The name of the transformation.
     * @param version The version of the transformation.
     */
    public static WorkbookValidator.Manifest createManifest(
            final String excelFileName,
            final String author,
            final String name,
            final Integer version,
            final UUID id
    ) throws IOException {
        Workbook workbook = WorkbookFactory.create(new File(excelFileName));
        return createManifest(workbook, author, name, version, id);
    }

    /**
     * Validates an Excel workbook and creates the manifest from an Excel file's {@link InputStream}.
     *
     * @param excelInputStream The {@link InputStream} of the Excel workbook.
     * @param author The author of the workbook.
     * @param name The name of the transformation.
     * @param version The version of the transformation.
     * @return {@link io.eel.common.WorkbookValidator.Manifest}
     */
    public static WorkbookValidator.Manifest createManifest(
        final InputStream excelInputStream,
        final String author,
        final String name,
        final Integer version,
        final UUID id
    ) throws IOException {
        Workbook workbook = WorkbookFactory.create(excelInputStream);
        return createManifest(workbook, author, name, version, id);
    }

    private static WorkbookValidator.Manifest createManifest(
        final Workbook workbook,
        final String author,
        final String name,
        final Integer version,
        final UUID id
    ) {
        return new WorkbookValidator(workbook, author, name, version, id)
                .assertIsValid()
                .createManifest();
    }

    private static final String EEL_XLSX_FILE_NAME = "eel.xlsx";

    /**
     * The platform-dependent temp directory path.
     */
    private static final String TMP_PATH = System.getProperty("java.io.tmpdir");

    public static File build(InputStream originalJarInputStream, InputStream excelInputStream) {
        try {
            // Write the original EEL JAR from to a tmp file.
            File jarTmpFile = new File(TMP_PATH + "/NEW_EEL.jar");
            Files.copy(originalJarInputStream, jarTmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            originalJarInputStream.close();

            log.info("Wrote EEL.jar to " + jarTmpFile.toPath());

            // Write the xlsx file to a tmp file.
            File tmpFile = new File(TMP_PATH + "/" + EEL_XLSX_FILE_NAME);
            Files.copy(excelInputStream, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            excelInputStream.close();

            // Write the manifest to a tmp file.
            final WorkbookValidator.Manifest manifest = createManifest(new FileInputStream(tmpFile), "me", "myEel", 0, UUID.randomUUID());
            final String manifestJson = gson.toJson(manifest);
            log.info(manifestJson);

            File tmpManifestFile = new File(TMP_PATH + "/manifest.json");
            Files.write(tmpManifestFile.toPath(), manifestJson.getBytes());
            log.info("Wrote manifest JSON to " + tmpManifestFile.getAbsolutePath());

            // Add manifest to the JAR.
            final String jarTmpFilePath = jarTmpFile.toPath().toAbsolutePath().toString();
            Process manifestCopyProcess = Runtime.getRuntime()
                    .exec(
                            new String[] {
                                    // Add manifest to JAR's resources.
                                    "jar", "uf", jarTmpFilePath, "-C", TMP_PATH, tmpManifestFile.getName()
                            }
                    );
            await(manifestCopyProcess);

            // Add xlsx file to the JAR.
            await(
                    Runtime.getRuntime().exec(
                            new String[] {
                                    "jar", "uf", jarTmpFilePath, "-C", TMP_PATH, tmpFile.getName()
                            }
                    )
            );

            // Save JAR to S3.
            log.info("EEL JAR is located at: " + jarTmpFilePath);

            return jarTmpFile;
        } catch (Throwable t) {
            // todo:  Add logic here.
            t.printStackTrace();

            throw new RuntimeException(t);
        }
    }

    private static void await(Process process) throws InterruptedException {
        int maxCounts = 3;
        int counts = 0;
        while (process.isAlive()) {
            if (counts >= maxCounts) {
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

    private static String getProcessErrorOutput(Process process) {
        try (BufferedReader reader = process.errorReader()) {
            return reader.lines()
                    .collect(Collectors.joining());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
