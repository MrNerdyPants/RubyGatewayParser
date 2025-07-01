package service;

import model.RubyMethodMetadata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the service.RubyMethodParser class in the RubyTransformation project.
 *
 * @author Kashan Asim
 * @version 1.0
 * @project RubyTransformation
 * @module PACKAGE_NAME
 * @class service.RubyMethodParser
 * @lastModifiedBy Kashan.Asim
 * @lastModifiedDate 6/29/2025
 * @license Licensed under the Apache License, Version 2.0
 * @description A brief description of the class functionality.
 * @notes <ul>
 * <li>Provide any additional notes or remarks here.</li>
 * </ul>
 * @since 6/29/2025
 */
public class RubyMethodParser {

    public static RubyMethodMetadata parseRubyMethod(String defBlock, String providedBackendVersion) {
        RubyMethodMetadata meta = new RubyMethodMetadata();

        // 1. Method name
        Matcher nameMatcher = Pattern.compile("def\\s+(?:self\\.)?(\\w+)").matcher(defBlock);
//                Pattern.compile("def\\s+(\\w+)").matcher(defBlock);
        if (nameMatcher.find()) {
            meta.methodName = nameMatcher.group(1);
        }

        // 2. Query params e.g., query = {MSISDN: number, UID: uid}
        Matcher queryMatcher = Pattern.compile("query\\s*=\\s*\\{([^}]+)}").matcher(defBlock);
        if (queryMatcher.find()) {
            String[] fields = queryMatcher.group(1).split(",");
            for (String field : fields) {
                String[] parts = field.trim().split(":");
                if (parts.length > 0) {
                    meta.queryParams.add(parts[0].trim());
                }
            }
        }

        queryMatcher = Pattern.compile("params\\s*=\\s*\\{(.*?)\\}", Pattern.DOTALL).matcher(defBlock);
        if (queryMatcher.find()) {
            String hashBody = queryMatcher.group(1);

            // Match keys like `MSISDN:`, `FirstName:`, etc.
            Matcher keyMatcher = Pattern.compile("(\\w+)\\s*:").matcher(hashBody);
            while (keyMatcher.find()) {
                meta.queryParams.add(keyMatcher.group(1));
            }
        }



        // 3. Router block
//        Matcher routerMatcher = Pattern.compile("micro_service:\\s*\"([^\"]+)\".*?operation:\\s*\"([^\"]+)\".*?backend_ver:\\s*(.+?)(,|\\n|\\})", Pattern.DOTALL).matcher(defBlock);
        Pattern routerPattern = Pattern.compile(
                "micro_service:\\s*\"([^\"]+)\".*?" +
                        "operation:\\s*\"([^\"]*)\".*?" +
                        "backend_ver:\\s*(?:\"([^\"]+)\"|(\\w+))",
                Pattern.DOTALL
        );
        Matcher routerMatcher = routerPattern.matcher(defBlock);
        if (routerMatcher.find()) {
            meta.microService = routerMatcher.group(1);
            meta.operation = routerMatcher.group(2);

            String backendRaw = "";
            try{
                backendRaw = routerMatcher.group(3).trim();
            }
            catch (Exception e){
                backendRaw = routerMatcher.group(4).trim();
            }
            if (backendRaw.equals("BACKEND_VERSION")) {
                meta.backendVersion = providedBackendVersion;
            } else {
                meta.backendVersion = backendRaw.replaceAll("\"", "").replaceAll("'", "");
            }

            meta.endpoint = meta.microService + "/" + meta.backendVersion + "/" + meta.operation;
        }

        // 4. Response parsing method
//        Matcher responseMatcher = Pattern.compile("Virgin::API::Response\\.new\\(.*?\\)\\.([a-zA-Z0-9_]+)").matcher(defBlock);
        Matcher responseMatcher = Pattern.compile(
                "Virgin::API::(?:Response|CommitResponse|ResponseV2)\\.new\\(.*?\\)\\.([a-zA-Z0-9_]+)"
        ).matcher(defBlock);
        if (responseMatcher.find()) {
            meta.responseUnwrapMethod = responseMatcher.group(1);
        }

        String httpMethod = extractHttpMethod(defBlock);
        meta.httpMethod = httpMethod;


        return meta;
    }

    private static String extractHttpMethod(String defBlock) {
        Pattern pattern = Pattern.compile("self\\.class\\.(get|post|put|delete|patch)\\s*\\(");
        Matcher matcher = pattern.matcher(defBlock);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase(); // Return as GET, POST, etc.
        }
        return null;
    }
}

