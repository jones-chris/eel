package io.eel.packager;

import com.google.gson.Gson;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class EelPackager {

    private static final Gson gson = new Gson();

    /**
     * An alternative entry point that exposes the {@link EelPackager#pack(String, String, String, Integer)} method
     * to scripts.
     *
     * @param args {@link String[]}
     */
    public static void main(String[] args) throws IOException {
        System.out.println(Arrays.toString(args));

        // Get Excel file name.
        final String excelFileName = args[0];
        Workbook workbook = WorkbookFactory.create(new File(excelFileName));

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

        pack(excelFileName, author, name, version);
    }

    /**
     * Validates an Excel workbook and creates the manifest from an Excel file's {@link String} path.
     *
     * @param excelFileName The file path {@link String} of the Excel workbook.
     * @param author The author of the workbook.
     * @param name The name of the transformation.
     * @param version The version of the transformation.
     */
    public static Path pack(
            final String excelFileName,
            final String author,
            final String name,
            final Integer version
    ) throws IOException {
        Workbook workbook = WorkbookFactory.create(new File(excelFileName));
        return pack(workbook, author, name, version);
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
    public static Path pack(
        final InputStream excelInputStream,
        final String author,
        final String name,
        final Integer version
    ) throws IOException {
        Workbook workbook = WorkbookFactory.create(excelInputStream);
        return pack(workbook, author, name, version);
    }

    private static Path pack(
        final Workbook workbook,
        final String author,
        final String name,
        final Integer version
    ) throws IOException {
        final WorkbookValidator.Manifest manifest = new WorkbookValidator(workbook, author, name, version)
                .assertIsValid()
                .createManifest();

        final String manifestJson = gson.toJson(manifest);

        Path path = Paths.get("eel_manifest.json");
        Files.write(path, manifestJson.getBytes());

        return path;
    }

}
