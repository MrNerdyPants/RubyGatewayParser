package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ApiMetadata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the service.DescBlockParser class in the RubyTransformation project.
 *
 * @author Kashan Asim
 * @version 1.0
 * @project RubyTransformation
 * @module PACKAGE_NAME
 * @class service.DescBlockParser
 * @lastModifiedBy Kashan.Asim
 * @lastModifiedDate 6/28/2025
 * @license Licensed under the Apache License, Version 2.0
 * @description A brief description of the class functionality.
 * @notes <ul>
 * <li>Provide any additional notes or remarks here.</li>
 * </ul>
 * @since 6/28/2025
 */
public class DescBlockParser {

    public static ApiMetadata parseDescBlock(String block, String fileBaseName, String wholeFileContent) {
        ApiMetadata metadata = new ApiMetadata();

        // 1. Extract API Name
        Matcher apiNameMatcher = Pattern.compile("desc\\s+'([^']+)'").matcher(block);
        if (apiNameMatcher.find()) {
            metadata.apiName = apiNameMatcher.group(1);
        }

        // 2. Headers
        Matcher headerMatcher = Pattern.compile("'([^']+)'\\s*=>\\s*\\{\\s*description:").matcher(block);
        while (headerMatcher.find()) {
            metadata.headers.add(headerMatcher.group(1));
        }

        // 3. Params
        Matcher paramsBlockMatcher = Pattern.compile("params\\s+do(.*?)end", Pattern.DOTALL).matcher(block);
        if (paramsBlockMatcher.find()) {
            String paramsBlock = paramsBlockMatcher.group(1);
            metadata.jsonBody = buildJsonFromParams(paramsBlock);
        }

        // 4. HTTP Method and Path
//        Matcher methodMatcher = Pattern.compile("(post|get|put|delete)\\s+:([\\w_]+)").matcher(block);
        Matcher methodMatcher = Pattern.compile("(post|get|put|delete)\\s+[:'\"]?([\\w_]+)").matcher(block);
        if (methodMatcher.find()) {
            String subPath = methodMatcher.group(2);
            metadata.httpMethod = methodMatcher.group(1).toUpperCase();
            metadata.endpoint = fileBaseName + "/" + (subPath.equalsIgnoreCase("do") ? "" : subPath);
        }

        // 5. Southbound Version - Option A: Check 'before' tag
        Matcher beforeMatcher = Pattern.compile("before\\s*\\{[^}]*Virgin::API::(V\\d+)::").matcher(wholeFileContent);
        if (beforeMatcher.find()) {
            metadata.southboundVersion = beforeMatcher.group(1);
        } else {
            // Option B: Look inside 'create_client(...)' inside the block
            Matcher versionMatcher = Pattern.compile("create_client\\(Virgin::API::(V\\d+)::").matcher(block);
            if (versionMatcher.find()) {
                metadata.southboundVersion = versionMatcher.group(1);
            }
        }

        // 6. Southbound method: client.some_method_name(...)
//        Matcher sbMethodMatcher = Pattern.compile("client\\.([a-zA-Z0-9_]+)\\s*\\(").matcher(block);
        Matcher sbMethodMatcher = Pattern.compile("@?client\\.([a-zA-Z0-9_]+)\\b").matcher(block);
        if (sbMethodMatcher.find()) {
            metadata.southboundMethod = sbMethodMatcher.group(1);
        }

        return metadata;
    }


//    public static model.ApiMetadata parseDescBlock(String block, String filenameWithoutExtension) {
//        model.ApiMetadata metadata = new model.ApiMetadata();
//
//        // 1. Extract API name
//        Matcher apiNameMatcher = Pattern.compile("desc\\s+'([^']+)'").matcher(block);
//        if (apiNameMatcher.find()) {
//            metadata.apiName = apiNameMatcher.group(1);
//        }
//
//        // 2. Extract headers
//        Matcher headerMatcher = Pattern.compile("'([^']+)'\\s*=>\\s*\\{\\s*description:").matcher(block);
//        while (headerMatcher.find()) {
//            metadata.headers.add(headerMatcher.group(1));
//        }
//
//        // 3. Extract params and generate JSON body
//        Matcher paramsBlockMatcher = Pattern.compile("params\\s+do(.*?)end", Pattern.DOTALL).matcher(block);
//        if (paramsBlockMatcher.find()) {
//            String paramsBlock = paramsBlockMatcher.group(1);
//            metadata.jsonBody = buildJsonFromParams(paramsBlock);
//        }
//
//        // 4. Extract HTTP method and API path
//        Matcher methodMatcher = Pattern.compile("(post|get|put|delete)\\s+:([\\w_]+)").matcher(block);
//        if (methodMatcher.find()) {
//            metadata.httpMethod = methodMatcher.group(1).toUpperCase();
//            metadata.endpoint = filenameWithoutExtension + "/" + methodMatcher.group(2);
//        }
//
//        return metadata;
//    }



    // Alternative method that processes line by line
    private static String buildJsonFromParams(String paramsBlock) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        String[] lines = paramsBlock.split("\\r?\\n");
        System.out.println("=== LINE BY LINE PROCESSING ===");
        System.out.println("Total lines: " + lines.length);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            System.out.println("Line " + i + ": '" + line + "'");

            if (line.isEmpty()) continue;

            // Pattern for a single line
            Pattern linePattern = Pattern.compile("(optional|requires)\\s+:([\\w_]+),\\s*type:\\s*(\\w+)");
            Matcher lineMatcher = linePattern.matcher(line);

            if (lineMatcher.find()) {
                String name = lineMatcher.group(2);
                String type = lineMatcher.group(3);

                System.out.println("  -> Found: " + name + " (type: " + type + ")");

                switch (type.toLowerCase()) {
                    case "string":
                        root.put(name, "example");
                        break;
                    case "boolean":
                        root.put(name, true);
                        break;
                    case "integer":
                        root.put(name, 123);
                        break;
                    case "float":
                        root.put(name, 1.23);
                        break;
                    default:
                        root.put(name, "value");
                }
            } else {
                System.out.println("  -> No match found for this line");
            }
        }

        return root.toPrettyString();
    }


//    private static String buildJsonFromParams(String paramsBlock) {
//        ObjectMapper mapper = new ObjectMapper();
//        ObjectNode root = mapper.createObjectNode();
//
//        // More flexible pattern that handles various whitespace and quote types
//        Pattern pattern = Pattern.compile("(?:optional|requires)\\s*:\\s*([\\w_]+)\\s*,\\s*type\\s*:\\s*(\\w+)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
//        Matcher matcher = pattern.matcher(paramsBlock);
//
//        while (matcher.find()) {
//            String name = matcher.group(1);
//            String type = matcher.group(2);
//
//            System.out.println("Found: " + name + " -> " + type);
//
//            switch (type.toLowerCase()) {
//                case "string":
//                    root.put(name, "example");
//                    break;
//                case "boolean":
//                    root.put(name, true);
//                    break;
//                case "integer":
//                    root.put(name, 123);
//                    break;
//                case "float":
//                    root.put(name, 1.23);
//                    break;
//                default:
//                    root.put(name, "value");
//            }
//        }
//
//        return root.toPrettyString();
//    }

//    private static String buildJsonFromParams(String paramsBlock) {
//        ObjectMapper mapper = new ObjectMapper();
//        ObjectNode root = mapper.createObjectNode();
//
//        // Updated pattern to handle the actual Ruby parameter format
//        // This pattern captures: optional/requires :param_name, type: Type
//        // and ignores everything after type: until the next line
//        Pattern pattern = Pattern.compile("(optional|requires)\\s+:([\\w_]+),\\s*type:\\s*(\\w+)", Pattern.MULTILINE);
//        Matcher matcher = pattern.matcher(paramsBlock);
//
//        while (matcher.find()) {
//            String name = matcher.group(2);
//            String type = matcher.group(3);
//
//            if(name.equalsIgnoreCase("marital_status")){
//                System.out.println("Found marital_status");
//            }
//
//            switch (type.toLowerCase()) {
//                case "string":
//                    root.put(name, "example");
//                    break;
//                case "boolean":
//                    root.put(name, true);
//                    break;
//                case "integer":
//                    root.put(name, 123);
//                    break;
//                case "float":
//                    root.put(name, 1.23);
//                    break;
//                default:
//                    root.put(name, "value");
//            }
//        }
//
//        return root.toPrettyString();
//    }

//    private static String buildJsonFromParams(String paramsBlock) {
//        ObjectMapper mapper = new ObjectMapper();
//        ObjectNode root = mapper.createObjectNode();
//
//        // This pattern matches both `optional` and `requires` with multi-line support
//        Pattern pattern = Pattern.compile("(optional|requires)\\s+:([\\w_]+),\\s*type:\\s*(\\w+)", Pattern.MULTILINE);
//        Matcher matcher = pattern.matcher(paramsBlock);
//
//        while (matcher.find()) {
//            String name = matcher.group(2);
//            String type = matcher.group(3);
//
//            if(name.equalsIgnoreCase("marital_status")){
//////                name = "BAKA USER";
//                System.out.println("ssdsd");
//            }
//
//            switch (type.toLowerCase()) {
//                case "string":
//                    root.put(name, "example");
//                    break;
//                case "boolean":
//                    root.put(name, true);
//                    break;
//                case "integer":
//                    root.put(name, 123);
//                    break;
//                case "float":
//                    root.put(name, 1.23);
//                    break;
//                default:
//                    root.put(name, "value");
//            }
//        }
//
//        return root.toPrettyString();
//    }



//    private static String buildJsonFromParams(String paramsBlock) {
//        ObjectMapper mapper = new ObjectMapper();
//        ObjectNode root = mapper.createObjectNode();
//
//        // Match both 'requires' and 'optional' with flexible spacing and line breaks
//        Matcher paramMatcher = Pattern.compile(
//                "(requires|optional)\\s+:([\\w_]+),\\s*type:\\s*(\\w+)",
//                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
//        ).matcher(paramsBlock);
//
//        while (paramMatcher.find()) {
//            String name = paramMatcher.group(2);
//            String type = paramMatcher.group(3);
//
//            switch (type.toLowerCase()) {
//                case "string":
//                    root.put(name, "example");
//                    break;
//                case "boolean":
//                    root.put(name, true);
//                    break;
//                case "integer":
//                    root.put(name, 123);
//                    break;
//                case "float":
//                    root.put(name, 1.23);
//                    break;
//                default:
//                    root.put(name, "value");
//            }
//        }
//
//        return root.toPrettyString();
//    }



//    private static String buildJsonFromParams(String paramsBlock) {
//        ObjectMapper mapper = new ObjectMapper();
//        ObjectNode root = mapper.createObjectNode();
//
//        // Match both 'requires' and 'optional'
//        Pattern paramPattern = Pattern.compile(
//                "(requires|optional)\\s+:([\\w_]+),\\s*type:\\s*(\\w+)(?:,\\s*default:\\s*([^,\\n]+))?(?:,\\s*desc:\\s*'([^']*)')?",
//                Pattern.DOTALL
//        );
//
//        Matcher paramMatcher = paramPattern.matcher(paramsBlock);
//        while (paramMatcher.find()) {
//            String paramType = paramMatcher.group(1); // 'requires' or 'optional' — you can use this later if needed
//            String name = paramMatcher.group(2);
//            String type = paramMatcher.group(3);
//            String defaultValue = paramMatcher.group(4); // optional, can be null
//            // String desc = paramMatcher.group(5); // optional, use if you want descriptions
//            if(name.equalsIgnoreCase("plan_code") || name.equalsIgnoreCase("marital_status")){
////                name = "BAKA USER";
//                System.out.println("ssdsd");
//            }
//
//
//            switch (type.toLowerCase()) {
//                case "string":
//                    root.put(name, defaultValue != null ? defaultValue.replaceAll("\"", "").trim() : "example");
//                    break;
//                case "boolean":
//                    root.put(name, defaultValue != null ? Boolean.parseBoolean(defaultValue.trim()) : true);
//                    break;
//                case "integer":
//                    root.put(name, defaultValue != null ? Integer.parseInt(defaultValue.trim()) : 123);
//                    break;
//                case "float":
//                    root.put(name, defaultValue != null ? Float.parseFloat(defaultValue.trim()) : 1.23f);
//                    break;
//                default:
//                    root.put(name, "value");
//            }
//        }
//
//        return root.toPrettyString();
//    }



//    private static String buildJsonFromParams(String paramsBlock) {
//        ObjectMapper mapper = new ObjectMapper();
//        ObjectNode root = mapper.createObjectNode();
//
//        Matcher paramMatcher = Pattern.compile("requires\\s+:([\\w_]+),\\s*type:\\s*(\\w+)", Pattern.DOTALL).matcher(paramsBlock);
//        while (paramMatcher.find()) {
//            String name = paramMatcher.group(1);
//            String type = paramMatcher.group(2);
//
//            switch (type.toLowerCase()) {
//                case "string":
//                    root.put(name, "example");
//                    break;
//                case "boolean":
//                    root.put(name, true);
//                    break;
//                case "integer":
//                    root.put(name, 123);
//                    break;
//                case "float":
//                    root.put(name, 1.23);
//                    break;
//                default:
//                    root.put(name, "value");
//            }
//        }
//
//        return root.toPrettyString();
//    }
}
