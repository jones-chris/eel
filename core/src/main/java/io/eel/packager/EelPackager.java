package io.eel.packager;

import com.google.gson.Gson;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class EelPackager {

    private static final Gson gson = new Gson();

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

        // Get author.
        String author;
        try {
            author = args[1];
        } catch (IndexOutOfBoundsException e) {
            author = System.getProperty("user.name");
        }

        // Get name.
        final String name = args[2];

        // Get version.
        final int version = Integer.parseInt(args[3]);

        // Generate the manifest, serialize it to JSON, and write it to a file.
        WorkbookValidator.Manifest manifest = createManifest(excelFileName, author, name, version);

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
            final Integer version
    ) throws IOException {
        Workbook workbook = WorkbookFactory.create(new File(excelFileName));
        return createManifest(workbook, author, name, version);
    }

    /**
     * Validates an Excel workbook and creates the manifest from an Excel file's {@link InputStream}.
     *
     * @param excelInputStream The {@link InputStream} of the Excel workbook.
     * @param author The author of the workbook.
     * @param name The name of the transformation.
     * @param version The version of the transformation.
     * @return The {@link Path} of the {@link io.eel.packager.WorkbookValidator.Manifest} file.
     */
    public static WorkbookValidator.Manifest createManifest(
        final InputStream excelInputStream,
        final String author,
        final String name,
        final Integer version
    ) throws IOException {
        Workbook workbook = WorkbookFactory.create(excelInputStream);
        return createManifest(workbook, author, name, version);
    }

    private static WorkbookValidator.Manifest createManifest(
        final Workbook workbook,
        final String author,
        final String name,
        final Integer version
    ) {
        return new WorkbookValidator(workbook, author, name, version)
                .assertIsValid()
                .createManifest();
    }

}
