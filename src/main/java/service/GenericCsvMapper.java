package service; /**
 * Represents the service.GenericCsvMapper class in the RubyTransformation project.
 *
 * @author Kashan Asim
 * @version 1.0
 * @project RubyTransformation
 * @module PACKAGE_NAME
 * @class service.GenericCsvMapper
 * @lastModifiedBy Kashan.Asim
 * @lastModifiedDate 6/29/2025
 * @license Licensed under the Apache License, Version 2.0
 * @description A brief description of the class functionality.
 * @notes <ul>
 * <li>Provide any additional notes or remarks here.</li>
 * </ul>
 * @since 6/29/2025
 */
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

public class GenericCsvMapper {

    /**
     * Generic method to write any list of objects to CSV
     * @param objectList List of objects to write to CSV
     * @param fileName Output CSV file name
     * @param clazz Class type of the objects
     */
    public static <T> void writeToCsv(List<T> objectList, String fileName, Class<T> clazz) throws IOException {
        if (objectList == null || objectList.isEmpty()) {
            throw new IllegalArgumentException("Object list cannot be null or empty");
        }

        try (FileWriter writer = new FileWriter(fileName)) {
            Field[] fields = clazz.getDeclaredFields();

            // Write CSV header using field names
            writeHeader(writer, fields);

            // Write data rows
            for (T obj : objectList) {
                writeDataRow(writer, obj, fields);
            }
        }
    }

    /**
     * Overloaded method that infers class type from the first object
     */
    @SuppressWarnings("unchecked")
    public static <T> void writeToCsv(List<T> objectList, String fileName) throws IOException {
        if (objectList == null || objectList.isEmpty()) {
            throw new IllegalArgumentException("Object list cannot be null or empty");
        }

        Class<T> clazz = (Class<T>) objectList.get(0).getClass();
        writeToCsv(objectList, fileName, clazz);
    }

    /**
     * Writes CSV header row using field names
     */
    private static void writeHeader(FileWriter writer, Field[] fields) throws IOException {
        for (int i = 0; i < fields.length; i++) {
            writer.append(fields[i].getName());
            if (i < fields.length - 1) {
                writer.append(",");
            }
        }
        writer.append("\n");
    }

    /**
     * Writes a single data row to CSV
     */
    private static <T> void writeDataRow(FileWriter writer, T obj, Field[] fields) throws IOException {
        for (int i = 0; i < fields.length; i++) {
            try {
                fields[i].setAccessible(true); // Allow access to private fields
                Object value = fields[i].get(obj);
                String csvValue = convertToString(value);
                writer.append(escapeCsvValue(csvValue));

                if (i < fields.length - 1) {
                    writer.append(",");
                }
            } catch (IllegalAccessException e) {
                writer.append(""); // Write empty string if field access fails
                if (i < fields.length - 1) {
                    writer.append(",");
                }
            }
        }
        writer.append("\n");
    }


    private static String convertToString(Object value) {
        if (value == null) {
            return "";
        }

        // Handle collections (like List<String>)
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            if (collection.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Object item : collection) {
                if (!first) {
                    sb.append(";");
                }
                sb.append(item != null ? item.toString() : "");
                first = false;
            }
            return sb.toString();
        }

        // Remove newlines and extra whitespace from JSON strings
        String stringValue = value.toString();
        if (stringValue.trim().startsWith("{") || stringValue.trim().startsWith("[")) {
            // This looks like JSON, minify it
            stringValue = stringValue.replaceAll("\\s+", " ").trim();
        }

        return stringValue;
    }


//    /**
//     * Converts object values to string representation suitable for CSV
//     */
//    private static String convertToString(Object value) {
//        if (value == null) {
//            return "";
//        }
//
//        // Handle collections (like List<String>)
//        if (value instanceof Collection) {
//            Collection<?> collection = (Collection<?>) value;
//            if (collection.isEmpty()) {
//                return "";
//            }
//
//            StringBuilder sb = new StringBuilder();
//            boolean first = true;
//            for (Object item : collection) {
//                if (!first) {
//                    sb.append(";");
//                }
//                sb.append(item != null ? item.toString() : "");
//                first = false;
//            }
//            return sb.toString();
//        }
//
//        return value.toString();
//    }

    /**
     * Escapes CSV values by wrapping in quotes if they contain commas, quotes, or newlines
     */
    private static String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }

        // If value contains comma, quote, or newline, wrap in quotes and escape internal quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

//    /**
//     * Example usage demonstrating the generic approach
//     */
//    public static void main(String[] args) {
//        try {
//            // Create sample model.RubyMethodMetadata objects
//            List<model.RubyMethodMetadata> rubyMetadataList = new ArrayList<>();
//
//            model.RubyMethodMetadata ruby1 = new model.RubyMethodMetadata();
//            ruby1.southBoundVersion = "v1.0";
//            ruby1.methodName = "getUserData";
//            ruby1.queryParams.add("userId");
//            ruby1.queryParams.add("includeProfile");
//            ruby1.microService = "user-service";
//            ruby1.operation = "getUser";
//            ruby1.backendVersion = "v2.1";
//            ruby1.endpoint = "user-service/v2.1/getUser";
//            ruby1.responseUnwrapMethod = "unwrapUserResponse";
//            ruby1.httpMethod = "GET";
//
//            model.RubyMethodMetadata ruby2 = new model.RubyMethodMetadata();
//            ruby2.southBoundVersion = "v1.1";
//            ruby2.methodName = "updateUser";
//            ruby2.queryParams.add("userId");
//            ruby2.microService = "user-service";
//            ruby2.operation = "updateUser";
//            ruby2.backendVersion = "v2.2";
//            ruby2.endpoint = "user-service/v2.2/updateUser";
//            ruby2.responseUnwrapMethod = "unwrapUpdateResponse";
//            ruby2.httpMethod = "PUT";
//
//            rubyMetadataList.add(ruby1);
//            rubyMetadataList.add(ruby2);
//
//            // Create sample model.ApiMetadata objects
//            List<model.ApiMetadata> apiMetadataList = new ArrayList<>();
//
//            model.ApiMetadata api1 = new model.ApiMetadata();
//            api1.apiName = "UserAPI";
//            api1.headers.add("Authorization");
//            api1.headers.add("Content-Type");
//            api1.httpMethod = "POST";
//            api1.endpoint = "/api/users";
//            api1.jsonBody = "{\"name\":\"John\",\"email\":\"john@example.com\"}";
//            api1.southboundVersion = "v1.0";
//            api1.southboundMethod = "createUser";
//
//            model.ApiMetadata api2 = new model.ApiMetadata();
//            api2.apiName = "OrderAPI";
//            api2.headers.add("Authorization");
//            api2.httpMethod = "GET";
//            api2.endpoint = "/api/orders";
//            api2.jsonBody = null;
//            api2.southboundVersion = "v2.0";
//            api2.southboundMethod = "getOrders";
//
//            apiMetadataList.add(api1);
//            apiMetadataList.add(api2);
//
//            // Write to CSV files using generic method
//            writeToCsv(rubyMetadataList, "ruby_method_metadata.csv");
//            writeToCsv(apiMetadataList, "api_metadata.csv");
//
//            // Alternative: Explicitly specify class type
//            // writeToCsv(rubyMetadataList, "ruby_method_metadata.csv", model.RubyMethodMetadata.class);
//            // writeToCsv(apiMetadataList, "api_metadata.csv", model.ApiMetadata.class);
//
//            System.out.println("CSV files created successfully using generic method!");
//
//        } catch (IOException e) {
//            System.err.println("Error writing CSV files: " + e.getMessage());
//        }
//    }
}

