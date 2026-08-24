package io.eel.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.officeDocument.x2006.customProperties.CTProperty;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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
     * An alternative entry point that exposes the {@link EelPackager#createManifest)} method to scripts.
     *
     * @param args {@link String[]}
     */
    public static void main(String[] args) throws IOException {
        System.out.println(Arrays.toString(args));

        // Get Excel file name.
        final String excelFileName = args[0];

        final UUID id = UUID.randomUUID();

        // Generate the manifest, serialize it to JSON, and write it to a file.
        WorkbookValidator.Manifest manifest = createManifest(excelFileName, id);

        String manifestJson = gson.toJson(manifest);

        Path tmpFilePath = new File("manifest.json").toPath();
        Files.write(tmpFilePath, manifestJson.getBytes());
        System.out.println("Manifest written to: " + tmpFilePath.toAbsolutePath());
    }

    /**
     * Validates a XLSX workbook and creates the manifest from a XLSX file's {@link String} path.
     *
     * @param excelFileName The file path {@link String} of the Excel workbook.
     */
    public static WorkbookValidator.Manifest createManifest(
            final String excelFileName,
            final UUID id
    ) throws IOException {
        Workbook workbook = WorkbookFactory.create(new File(excelFileName));

        POIXMLProperties.CoreProperties coreProperties = ((XSSFWorkbook) workbook).getProperties().getCoreProperties();
        POIXMLProperties.CustomProperties customProperties = ((XSSFWorkbook) workbook).getProperties().getCustomProperties();

        // Fetch the specific metadata fields.  The following 3 fields are required...
        final String title = Optional.ofNullable(coreProperties.getTitle())
                .orElseThrow(() -> new IllegalArgumentException("XLSX file is missing the 'Title' property in its core properties"));

        final String description = Optional.ofNullable(coreProperties.getDescription())
                .orElseThrow(() -> new IllegalArgumentException("XLSX file is missing the 'Description' property in its core properties"));

        final String creator = Optional.ofNullable(customProperties.getProperty("Author"))
                .map(CTProperty::getLpwstr)
                .orElseThrow(() -> new IllegalArgumentException("XLSX file is missing the 'Author' property in its custom properties"));

        // ...and this property is optional - although some spreadsheet applications will increment it automatically, such
        // as LibreOffice.
        final int version = Optional.ofNullable(coreProperties.getRevision())
                        .map(Integer::parseInt)
                        .orElse(0);

        return createManifest(workbook, creator, title, version, id, description);
    }

    /**
     * Validates an Excel workbook and creates the manifest from an Excel file's {@link InputStream}.
     *
     * @param excelInputStream The {@link InputStream} of the Excel workbook.
     * @return {@link io.eel.common.WorkbookValidator.Manifest}
     */
    public static WorkbookValidator.Manifest createManifest(
        final InputStream excelInputStream,
        final UUID id
    ) throws IOException {
        Workbook workbook = WorkbookFactory.create(excelInputStream);

        // todo:  put this duplicated code in a helper method.
        POIXMLProperties.CoreProperties coreProperties = ((XSSFWorkbook) workbook).getProperties().getCoreProperties();
        POIXMLProperties.CustomProperties customProperties = ((XSSFWorkbook) workbook).getProperties().getCustomProperties();

        // Fetch the specific metadata fields
        final String title = coreProperties.getTitle();
        final String description = coreProperties.getDescription();
        final String creator = customProperties.getProperty("Author").getLpwstr();
        final int version = coreProperties.getRevision() != null ? Integer.parseInt(coreProperties.getRevision()) : 0;

        return createManifest(workbook, creator, title, version, id, description);
    }

    private static WorkbookValidator.Manifest createManifest(
        final Workbook workbook,
        final String author,
        final String name,
        final Integer version,
        final UUID id,
        final String description
    ) {
        return new WorkbookValidator(workbook, author, name, version, id, description)
                .assertIsValid()
                .createManifest();
    }

    private static final String EEL_XLSX_FILE_NAME = "eel.xlsx";

    /**
     * The platform-dependent temp directory path.
     */
    private static final String TMP_PATH = System.getProperty("java.io.tmpdir");

    public static File build(InputStream originalJarInputStream, InputStream excelInputStream) {
        return new File("/tmp/NEW_EEL.jar");
//        try {
//            // Write the original EEL JAR from to a tmp file.
//            File jarTmpFile = new File(TMP_PATH + "/NEW_EEL.jar");
//            Files.copy(originalJarInputStream, jarTmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//            originalJarInputStream.close();
//
//            log.info("Wrote EEL.jar to " + jarTmpFile.toPath());
//
//            // Write the xlsx file to a tmp file.
//            File tmpFile = new File(TMP_PATH + "/" + EEL_XLSX_FILE_NAME);
//            Files.copy(excelInputStream, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//            excelInputStream.close();
//
//            // Write the manifest to a tmp file.
//            final WorkbookValidator.Manifest manifest = createManifest(new FileInputStream(tmpFile), "me", "myEel", 0, UUID.randomUUID(), "");
//            final String manifestJson = gson.toJson(manifest);
//            log.info(manifestJson);
//
//            File tmpManifestFile = new File(TMP_PATH + "/manifest.json");
//            Files.write(tmpManifestFile.toPath(), manifestJson.getBytes());
//            log.info("Wrote manifest JSON to " + tmpManifestFile.getAbsolutePath());
//
//            // Add manifest to the JAR.
//            final String jarTmpFilePath = jarTmpFile.toPath().toAbsolutePath().toString();
//            Process manifestCopyProcess = Runtime.getRuntime()
//                    .exec(
//                            new String[] {
//                                    // Add manifest to JAR's resources.
//                                    "jar", "uf", jarTmpFilePath, "-C", TMP_PATH, tmpManifestFile.getName()
//                            }
//                    );
//            await(manifestCopyProcess);
//
//            // Add xlsx file to the JAR.
//            await(
//                    Runtime.getRuntime().exec(
//                            new String[] {
//                                    "jar", "uf", jarTmpFilePath, "-C", TMP_PATH, tmpFile.getName()
//                            }
//                    )
//            );
//
//            // Save JAR to S3.
//            log.info("EEL JAR is located at: " + jarTmpFilePath);
//
//            return jarTmpFile;
//        } catch (Throwable t) {
//            // todo:  Add logic here.
//            t.printStackTrace();
//
//            throw new RuntimeException(t);
//        }
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
