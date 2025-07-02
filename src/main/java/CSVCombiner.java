import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the CSVCombiner class in the RubyTransformation project.
 *
 * @author Kashan Asim
 * @version 1.0
 * @project RubyTransformation
 * @module PACKAGE_NAME
 * @class CSVCombiner
 * @lastModifiedBy Kashan.Asim
 * @lastModifiedDate 6/29/2025
 * @license Licensed under the Apache License, Version 2.0
 * @description A brief description of the class functionality.
 * @notes <ul>
 * <li>Provide any additional notes or remarks here.</li>
 * </ul>
 * @since 6/29/2025
 */
public class CSVCombiner {

    // Class to represent the first CSV structure
    static class ApiRecord {
        String northboundVersion;
        String apiName;
        String headers;
        String httpMethod;
        String endpoint;
        String jsonBody;
        String southboundVersion;
        String southboundMethod;

        public ApiRecord(String[] fields) {
            this.northboundVersion = fields[0];
            this.apiName = fields[1];
            this.headers = fields[2];
            this.httpMethod = fields[3];
            this.endpoint = fields[4];
            this.jsonBody = fields[5];
            this.southboundVersion = fields[6];
            this.southboundMethod = fields[7];
        }

        public String getKey() {
            return southboundVersion + "," + southboundMethod;
        }
    }

    // Class to represent the second CSV structure
    static class SouthboundRecord {
        String southBoundVersion;
        String methodName;
        String queryParams;
        String microService;
        String operation;
        String backendVersion;
        String endpoint;
        String responseUnwrapMethod;
        String httpMethod;

        public SouthboundRecord(String[] fields) {
            this.southBoundVersion = fields[0];
            this.methodName = fields[1];
            this.queryParams = fields[2];
            this.microService = fields[3];
            this.operation = fields[4];
            this.backendVersion = fields[5];
            this.endpoint = fields[6];
            this.responseUnwrapMethod = fields[7];
            this.httpMethod = fields[8];
        }

        public String getKey() {
            return southBoundVersion + "," + methodName;
        }
    }

    // Combined record class
    static class CombinedRecord {
        ApiRecord apiRecord;
        SouthboundRecord southboundRecord;

        public CombinedRecord(ApiRecord api, SouthboundRecord southbound) {
            this.apiRecord = api;
            this.southboundRecord = southbound;
        }

        public String toCsvString() {
            StringBuilder sb = new StringBuilder();

            // Fields from first CSV
            sb.append(escapeField(apiRecord.northboundVersion)).append(",");
            sb.append(escapeField(apiRecord.apiName)).append(",");
            sb.append(escapeField(apiRecord.headers)).append(",");
            sb.append(escapeField(apiRecord.httpMethod)).append(",");
            sb.append(escapeField(apiRecord.endpoint)).append(",");
            sb.append(escapeField(apiRecord.jsonBody)).append(",");
            sb.append(escapeField(apiRecord.southboundVersion)).append(",");
            sb.append(escapeField(apiRecord.southboundMethod)).append(",");

            // Fields from second CSV
            sb.append(escapeField(southboundRecord.southBoundVersion)).append(",");
            sb.append(escapeField(southboundRecord.methodName)).append(",");
            sb.append(escapeField(southboundRecord.queryParams)).append(",");
            sb.append(escapeField(southboundRecord.microService)).append(",");
            sb.append(escapeField(southboundRecord.operation)).append(",");
            sb.append(escapeField(southboundRecord.backendVersion)).append(",");
            sb.append(escapeField(southboundRecord.endpoint)).append(",");
            sb.append(escapeField(southboundRecord.responseUnwrapMethod)).append(",");
            sb.append(escapeField(southboundRecord.httpMethod));

            return sb.toString();
        }

        private String escapeField(String field) {
            if (field == null) return "";
            if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
                return "\"" + field.replace("\"", "\"\"") + "\"";
            }
            return field;
        }
    }

    public static void main(String[] args) {
        String file1Path = "northbound.csv";     // First CSV file path
        String file2Path = "southbound.csv"; // Second CSV file path
        String outputPath = "combined_records.csv";   // Output file path

        try {
            List<CombinedRecord> combinedRecords = combineCSVFiles(file1Path, file2Path);
            writeCombinedCSV(combinedRecords, outputPath);

            System.out.println("Successfully combined CSV files!");
            System.out.println("Total combined records: " + combinedRecords.size());
            System.out.println("Output written to: " + outputPath);

        } catch (IOException e) {
            System.err.println("Error processing CSV files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static List<CombinedRecord> combineCSVFiles(String file1Path, String file2Path) throws IOException {
        // Read first CSV and create a map with key = southboundVersion,southboundMethod
        Map<String, ApiRecord> apiRecords = new HashMap<>();

        try (BufferedReader br1 = new BufferedReader(new FileReader(file1Path))) {
            String line = br1.readLine(); // Skip header
            while ((line = br1.readLine()) != null) {
                String[] fields = parseCSVLine(line);
                if (fields.length >= 7) {
                    ApiRecord record = new ApiRecord(fields);
                    apiRecords.put(record.getKey(), record);
                }
            }
        }

        // Read second CSV and create combined records
        List<CombinedRecord> combinedRecords = new ArrayList<>();

        try (BufferedReader br2 = new BufferedReader(new FileReader(file2Path))) {
            String line = br2.readLine(); // Skip header
            while ((line = br2.readLine()) != null) {
                String[] fields = parseCSVLine(line);
                if (fields.length >= 9) {
                    SouthboundRecord southboundRecord = new SouthboundRecord(fields);
                    String key = southboundRecord.getKey();

                    // Find matching API record
                    ApiRecord matchingApiRecord = apiRecords.get(key);
                    if (matchingApiRecord != null) {
                        combinedRecords.add(new CombinedRecord(matchingApiRecord, southboundRecord));
                    } else {
                        System.out.println("No matching API record found for key: " + key);
                    }
                }
            }
        }

        return combinedRecords;
    }

    public static void writeCombinedCSV(List<CombinedRecord> records, String outputPath) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(outputPath))) {
            // Write header
            pw.println("northboundVersion,/apiName,headers,httpMethod_api,endpoint_api,jsonBody,southboundVersion,southboundMethod," +
                    "southBoundVersion_sb,methodName,queryParams,microService,operation,backendVersion," +
                    "endpoint_sb,responseUnwrapMethod,httpMethod_sb");

            // Write combined records
            for (CombinedRecord record : records) {
                pw.println(record.toCsvString());
            }
        }
    }

    // Simple CSV parser that handles quoted fields
    public static String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Handle escaped quote
                    currentField.append('"');
                    i++; // Skip next quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        fields.add(currentField.toString());
        return fields.toArray(new String[0]);
    }

    // Utility method to create sample CSV files for testing
    public static void createSampleFiles() throws IOException {
        // Create sample first CSV
        try (PrintWriter pw = new PrintWriter(new FileWriter("api_records.csv"))) {
            pw.println("apiName,headers,httpMethod,endpoint,jsonBody,southboundVersion,southboundMethod");
            pw.println("getUserAPI,Content-Type: application/json,GET,/api/user,{},v1.0,getUser");
            pw.println("createUserAPI,Content-Type: application/json,POST,/api/user,{\"name\":\"test\"},v1.0,createUser");
            pw.println("updateUserAPI,Content-Type: application/json,PUT,/api/user/{id},{\"name\":\"updated\"},v2.0,updateUser");
        }

        // Create sample second CSV
        try (PrintWriter pw = new PrintWriter(new FileWriter("southbound_records.csv"))) {
            pw.println("southBoundVersion,methodName,queryParams,microService,operation,backendVersion,endpoint,responseUnwrapMethod,httpMethod");
            pw.println("v1.0,getUser,userId=123,UserService,READ,1.2,/users/get,unwrapUser,GET");
            pw.println("v1.0,createUser,,UserService,CREATE,1.2,/users/create,unwrapUser,POST");
            pw.println("v2.0,updateUser,userId=456,UserService,UPDATE,1.3,/users/update,unwrapUser,PUT");
        }

        System.out.println("Sample CSV files created successfully!");
    }
}
