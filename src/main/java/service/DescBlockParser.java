package service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ApiMetadata;

import java.util.ArrayList;
import java.util.List;
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

    public static ApiMetadata parseDescBlock(DescBlockExtractor.DescBlock block, String fileBaseName, String wholeFileContent) {
        ApiMetadata metadata = new ApiMetadata();

        // 1. Extract API Name
        Matcher apiNameMatcher = Pattern.compile("desc\\s+'([^']+)'").matcher(block.getDescBlock());
        if (apiNameMatcher.find()) {
            metadata.apiName = apiNameMatcher.group(1);
        }

        // 2. Headers
        Matcher headerMatcher = Pattern.compile("'([^']+)'\\s*=>\\s*\\{\\s*description:").matcher(block.getDescBlock());
        while (headerMatcher.find()) {
            metadata.headers.add(headerMatcher.group(1));
        }

        // 3. HTTP Method and Path
//        Matcher methodMatcher = Pattern.compile("(post|get|put|delete)\\s+:([\\w_]+)").matcher(block);
        Matcher methodMatcher = Pattern.compile("(post|get|put|delete)\\s+[:'\"]?([\\w_]+)").matcher(block.getDescBlock());
        if (methodMatcher.find()) {
            String subPath = methodMatcher.group(2);
            metadata.httpMethod = methodMatcher.group(1).toUpperCase();
            metadata.endpoint = (block.getResource() != null ? block.getResource() : fileBaseName) + "/" + (subPath.equalsIgnoreCase("do") ? "" : subPath);
        }


//        // 4. Params
//        Matcher paramsBlockMatcher = Pattern.compile("params\\s+do(.*?)end", Pattern.DOTALL).matcher(block.getDescBlock());
//                Pattern.compile("params\\s+do(.*?)end", Pattern.DOTALL).matcher(block.getDescBlock());
//        if (paramsBlockMatcher.find()) {
//            String paramsBlock = paramsBlockMatcher.group(1);
//            metadata.jsonBody = buildJsonFromParams(paramsBlock);
//        }
        String paramsBlock = extractParamsBlock(block.getDescBlock());
        if (paramsBlock != null) {
            metadata.jsonBody = buildJsonFromParams(paramsBlock, metadata.httpMethod);
        }

        // 5. Southbound Version - Option A: Check 'before' tag
        Matcher beforeMatcher = Pattern.compile("before\\s*\\{[^}]*Virgin::API::(V\\d+)::").matcher(wholeFileContent);
        if (beforeMatcher.find()) {
            metadata.southboundVersion = beforeMatcher.group(1);
        } else {
            // Option B: Look inside 'create_client(...)' inside the block
            Matcher versionMatcher = Pattern.compile("create_client\\(Virgin::API::(V\\d+)::").matcher(block.getDescBlock());
            Matcher versionMatcher2 = Pattern.compile("Virgin::API::(V\\d+)::").matcher(block.getDescBlock());
            if (versionMatcher.find()) {
                metadata.southboundVersion = versionMatcher.group(1);
            } else if (versionMatcher2.find()) {
                metadata.southboundVersion = versionMatcher2.group(1);
            }
        }

        // 6. Southbound method: client.some_method_name(...)
//        Matcher sbMethodMatcher = Pattern.compile("client\\.([a-zA-Z0-9_]+)\\s*\\(").matcher(block);
        Matcher sbMethodMatcher = Pattern.compile("@?client\\.([a-zA-Z0-9_]+)\\b").matcher(block.getDescBlock());
        if (sbMethodMatcher.find()) {
            metadata.southboundMethod = sbMethodMatcher.group(1);
        } else {
            metadata.southboundMethod = extractSouthboundMethod(block.getDescBlock());
        }

        return metadata;
    }


    private static String extractSouthboundMethod(String blockContent) {
        // Support:
        // - @client.MethodName(...)
        // - client.MethodName(...)
        // - CMSClient.MethodName(...)
        // - Virgin::API::V1::SomeClient.MethodName(...)
//        Pattern methodPattern = Pattern.compile(
//                "(?:(?:@?client)|(?:\\w+Client)|(?:Virgin::API::V\\d+::\\w+Client))\\.([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");
//        Matcher matcher = methodPattern.matcher(blockContent);
        Pattern sbMethodPattern = Pattern.compile(
                "(?:(?:@?client)|(?:\\w+Client)|(?:Virgin::API::V\\d+::\\w+Client))\\.(\\w+)\\s*\\(");
        Matcher matcher = sbMethodPattern.matcher(blockContent);

        if (matcher.find()) {
            return matcher.group(1); // method name
        }

        return null;
    }



//    private static String extractSouthboundMethod(String blockContent) {
//        // Match any expression ending with .methodName(...) where the receiver is
//        // either @client, client, SomeClient, or Virgin::API::Vx::SomeClient
//        Pattern methodPattern = Pattern.compile(
//                "(?:(?:@?client)|(?:\\w+Client)|(?:Virgin::API::V\\d+::\\w+Client))\\.([a-zA-Z0-9_]+)\\s*\\(");
//        Matcher matcher = methodPattern.matcher(blockContent);
//
//        if (matcher.find()) {
//            return matcher.group(1); // method name
//        }
//
//        return null;
//    }

//    private static String extractSouthboundMethod(String blockContent) {
//        // Covers @client.method, client.method, CMSClient.method, etc.
//        Pattern methodPattern = Pattern.compile("(?:@?client|CMSClient)\\.([a-zA-Z0-9_]+)\\s*\\(");
//        Matcher matcher = methodPattern.matcher(blockContent);
//        if (matcher.find()) {
//            return matcher.group(1);
//        }
//
//        // Optional fallback: methods without parentheses (e.g. `@client.activate`)
//        Pattern altPattern = Pattern.compile("(?:@?client|CMSClient)\\.([a-zA-Z0-9_]+)\\b");
//        Matcher altMatcher = altPattern.matcher(blockContent);
//        if (altMatcher.find()) {
//            return altMatcher.group(1);
//        }
//
//        return null;
//    }


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


    private static String extractParamsBlock(String input) {
        int paramsIndex = input.indexOf("params do");
        if (paramsIndex == -1) return null;

        int start = input.indexOf("do", paramsIndex);
        if (start == -1) return null;

        int doCount = 1;
        int i = start + 2; // skip initial 'do'
        int end = -1;

        while (i < input.length()) {
            if (input.startsWith("do", i) && isWordBoundary(input, i, 2)) {
                doCount++;
                i += 2;
            } else if (input.startsWith("end", i) && isWordBoundary(input, i, 3)) {
                doCount--;
                if (doCount == 0) {
                    end = i;
                    break;
                }
                i += 3;
            } else {
                i++;
            }
        }

        if (end != -1) {
            return input.substring(start + 2, end).trim(); // exclude outer 'do' and 'end'
        }

        return null;
    }

    private static boolean isWordBoundary(String input, int pos, int length) {
        boolean before = (pos == 0) || !Character.isLetterOrDigit(input.charAt(pos - 1));
        boolean after = (pos + length >= input.length()) || !Character.isLetterOrDigit(input.charAt(pos + length));
        return before && after;
    }


//    private static String extractParamsBlock(String input) {
//        if(input.contains("marital_status")){
//            System.out.println("sss");
//        }
//        int startIdx = input.indexOf("params do");
//        if (startIdx == -1) return null;
//
//        int doCount = 0;
//        boolean started = false;
//        int endIdx = -1;
//
//        for (int i = startIdx; i < input.length(); i++) {
//            String slice = input.substring(i);
//
//            if (slice.startsWith("do")) {
//                if (!started && input.substring(startIdx, i).contains("params")) {
//                    started = true;
//                }
//                if (started) doCount++;
//            } else if (slice.startsWith("end") && started) {
//                doCount--;
//                if (doCount == 0) {
//                    endIdx = i + 3; // include 'end'
//                    break;
//                }
//            }
//        }
//
//        if (endIdx != -1) {
//            return input.substring(startIdx + "params do".length(), endIdx - 3).trim(); // exclude outer 'params do' and 'end'
//        }
//        return null;
//    }


    // Alternative method that processes line by line
    private static String buildJsonFromParams(String paramsBlock, String httpMethod) {
        if (httpMethod == null) {
            httpMethod = "GET";
        }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        List<String> queryParams = new ArrayList<>();

        String[] lines = paramsBlock.split("\\r?\\n");
        System.out.println("=== LINE BY LINE PROCESSING ===");
        System.out.println("Total lines: " + lines.length);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            System.out.println("Line " + i + ": '" + line + "'");

            if (line.isEmpty()) continue;

            Pattern linePattern = Pattern.compile("(optional|requires)\\s+:([\\w_]+),\\s*type:\\s*(\\w+)", Pattern.CASE_INSENSITIVE);
            Matcher lineMatcher = linePattern.matcher(line);

            if (lineMatcher.find()) {
                String name = lineMatcher.group(2);
                String type = lineMatcher.group(3).toLowerCase();

                System.out.println("  -> Found: " + name + " (type: " + type + ")");

                String exampleValue;
                switch (type) {
                    case "string":
                        exampleValue = "example";
                        root.put(name, exampleValue);
                        break;
                    case "boolean":
                        exampleValue = "true";
                        root.put(name, true);
                        break;
                    case "integer":
                        exampleValue = "123";
                        root.put(name, 123);
                        break;
                    case "float":
                        exampleValue = "1.23";
                        root.put(name, 1.23);
                        break;
                    default:
                        exampleValue = "value";
                        root.put(name, exampleValue);
                }

                // Add to query params too
                queryParams.add(name + "=" + exampleValue);
            } else {
                System.out.println("  -> No match found for this line");
            }
        }

        if (httpMethod.equalsIgnoreCase("GET")) {
            return String.join("&", queryParams);
        } else {
            try {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return "{}";
            }
        }
    }

//    private static String buildJsonFromParams(String paramsBlock, String httpMethod) {
//        ObjectMapper mapper = new ObjectMapper();
//        ObjectNode root = mapper.createObjectNode();
//
//        String[] lines = paramsBlock.split("\\r?\\n");
//        System.out.println("=== LINE BY LINE PROCESSING ===");
//        System.out.println("Total lines: " + lines.length);
//
//        for (int i = 0; i < lines.length; i++) {
//            String line = lines[i].trim();
//            System.out.println("Line " + i + ": '" + line + "'");
//
//            if (line.isEmpty()) continue;
//
//            // Pattern for a single line
//            Pattern linePattern = Pattern.compile("(optional|requires)\\s+:([\\w_]+),\\s*type:\\s*(\\w+)");
//            Matcher lineMatcher = linePattern.matcher(line);
//
//            if (lineMatcher.find()) {
//                String name = lineMatcher.group(2);
//                String type = lineMatcher.group(3);
//
//                System.out.println("  -> Found: " + name + " (type: " + type + ")");
//
//                switch (type.toLowerCase()) {
//                    case "string":
//                        root.put(name, "example");
//                        break;
//                    case "boolean":
//                        root.put(name, true);
//                        break;
//                    case "integer":
//                        root.put(name, 123);
//                        break;
//                    case "float":
//                        root.put(name, 1.23);
//                        break;
//                    default:
//                        root.put(name, "value");
//                }
//            } else {
//                System.out.println("  -> No match found for this line");
//            }
//        }
//
//        return root.toPrettyString();
//    }


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
