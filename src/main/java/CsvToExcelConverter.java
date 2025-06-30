/**
 * Represents the CsvToExcelConverter class in the RubyTransformation project.
 *
 * @author Kashan Asim
 * @version 1.0
 * @project RubyTransformation
 * @module PACKAGE_NAME
 * @class CsvToExcelConverter
 * @lastModifiedBy Kashan.Asim
 * @lastModifiedDate 6/29/2025
 * @license Licensed under the Apache License, Version 2.0
 * @description A brief description of the class functionality.
 * @notes <ul>
 * <li>Provide any additional notes or remarks here.</li>
 * </ul>
 * @since 6/29/2025
 */
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CsvToExcelConverter {

    public static void convertCsvToExcel(String csvFilePath, String excelFilePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath));
             Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(excelFilePath)) {

            // Create a sheet
            Sheet sheet = workbook.createSheet("Data");

            String line;
            int rowNum = 0;

            // Read CSV line by line
            while ((line = br.readLine()) != null) {
                Row row = sheet.createRow(rowNum++);
                String[] values = parseCsvLine(line);

                // Create cells and set values
                for (int i = 0; i < values.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(values[i]);
                }
            }

            // Auto-size columns
            for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to Excel file
            workbook.write(fos);
            System.out.println("CSV converted to Excel successfully!");

        } catch (IOException e) {
            System.err.println("Error converting CSV to Excel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Simple CSV parser that handles quotes and commas
    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }

        result.add(sb.toString().trim());
        return result.toArray(new String[0]);
    }

    // Alternative method using OpenCSV library for more robust parsing
    public static void convertCsvToExcelWithOpenCSV(String csvFilePath, String excelFilePath) {
        /*
        // Uncomment if using OpenCSV dependency
        try (CSVReader csvReader = new CSVReader(new FileReader(csvFilePath));
             Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(excelFilePath)) {

            Sheet sheet = workbook.createSheet("Data");
            String[] nextLine;
            int rowNum = 0;

            while ((nextLine = csvReader.readNext()) != null) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < nextLine.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(nextLine[i]);
                }
            }

            // Auto-size columns
            for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(fos);
            System.out.println("CSV converted to Excel successfully with OpenCSV!");

        } catch (IOException | CsvException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        */
    }

    public static void main(String[] args) {
        String csvFile = "combined_records.csv";
        String excelFile = "output.xlsx";

        convertCsvToExcel(csvFile, excelFile);
    }
}
