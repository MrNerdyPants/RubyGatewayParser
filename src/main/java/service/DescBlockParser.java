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

    private static String buildJsonFromParams(String paramsBlock) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        Matcher paramMatcher = Pattern.compile("requires\\s+:([\\w_]+),\\s*type:\\s*(\\w+)", Pattern.DOTALL).matcher(paramsBlock);
        while (paramMatcher.find()) {
            String name = paramMatcher.group(1);
            String type = paramMatcher.group(2);

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
        }

        return root.toPrettyString();
    }
}
