package io.eel.service;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class ZipCsvProcessor {

    public static Map<String, Object[][]> unzipAndParseCsvs(ZipInputStream zipInputStream) throws IOException {
        Map<String, Object[][]> allCsvData = new HashMap<>();

        try (zipInputStream) {

            ZipEntry zipEntry;
            // 1. Iterate through each entry (file) in the ZIP
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (!zipEntry.isDirectory() && zipEntry.getName().toLowerCase().endsWith(".csv")) {
                    String fileName = zipEntry.getName();

                    // 2. Use a Reader pointing to the current ZipEntry's stream
                    InputStreamReader isr = new InputStreamReader(zipInputStream);

                    // 3. Use Apache Commons CSVParser to parse the data
                    // We define the format (e.g., standard CSV, skipping the header record)
                    CSVFormat csvFormat = CSVFormat.DEFAULT
                            .builder()
                            .setSkipHeaderRecord(true) // Set to true if you want to skip the header row
                            .build();

                    try (CSVParser parser = new CSVParser(isr, csvFormat)) {

                        List<String[]> records = new ArrayList<>();

                        // 4. Iterate over the CSV records
                        for (CSVRecord csvRecord : parser) {
                            // The CSVRecord gives you the values as a List of Strings
                            int columnCount = csvRecord.size();
                            String[] row = new String[columnCount];

                            for (int i = 0; i < columnCount; i++) {
                                row[i] = csvRecord.get(i);
                            }
                            records.add(row);
                        }

                        // 5. Convert the List of String arrays to Object[][]
                        Object[][] dataArray = convertTo2DObjectArray(records);
                        allCsvData.put(fileName, dataArray);
                    } catch (Throwable t) {
                        System.err.println("Error parsing CSV file " + fileName + ": " + t.getMessage());

                        throw t;
                    }
                }
                zipInputStream.closeEntry();
            }
        }
        return allCsvData;
    }

    // Helper function to convert List<String[]> to Object[][] (same as previous example)
    private static Object[][] convertTo2DObjectArray(List<String[]> records) {
        if (records.isEmpty()) {
            return new Object[0][0];
        }

        // Determine max number of columns based on the widest row
        int maxColumns = records.stream()
                .mapToInt(arr -> arr.length)
                .max()
                .orElse(0);

        Object[][] result = new Object[records.size()][maxColumns];

        for (int i = 0; i < records.size(); i++) {
            String[] row = records.get(i);
            // Copy String array to Object array
            System.arraycopy(row, 0, result[i], 0, row.length);
        }
        return result;
    }

//    // Example Usage:
//    public static void main(String[] args) {
//        // Assume 'data.zip' is available and contains csv files.
//        String path = "data.zip";
//        try {
//            Map<String, Object[][]> dataMap = unzipAndParseCsvs(path);
//
//            for (Map.Entry<String, Object[][]> entry : dataMap.entrySet()) {
//                System.out.println("--- Data for file: " + entry.getKey() + " ---");
//                Object[][] data = entry.getValue();
//                for (Object[] row : data) {
//                    System.out.println(Arrays.toString(row));
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}