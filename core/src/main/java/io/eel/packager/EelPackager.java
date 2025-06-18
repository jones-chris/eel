package io.eel.packager;

import com.google.gson.Gson;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EelPackager {

    private static final Gson gson = new Gson();

    public static void main(String[] args) throws IOException {
        final String excelFileName = args[0];
        Workbook workbook = WorkbookFactory.create(new File(excelFileName));

        String author;
        try {
            author = args[1];
        } catch (IndexOutOfBoundsException e) {
            author = System.getProperty("user.name");
        }

        final String name = args[2];

        final int version = Integer.parseInt(args[3]);

        final WorkbookValidator.Manifest manifest = new WorkbookValidator(workbook, author, name, version)
                .assertIsValid()
                .createManifest();

        final String manifestJson = gson.toJson(manifest);

        Path path = Paths.get("eel_manifest.json");
        Files.write(path, manifestJson.getBytes());
    }

}
