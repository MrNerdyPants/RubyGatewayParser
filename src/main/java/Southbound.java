import model.RubyMethodMetadata;
import service.GenericCsvMapper;
import service.RubyMethodParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the SouthboundDefParser class in the RubyTransformation project.
 *
 * @author Kashan Asim
 * @version 1.0
 * @project RubyTransformation
 * @module PACKAGE_NAME
 * @class SouthboundDefParser
 * @lastModifiedBy Kashan.Asim
 * @lastModifiedDate 6/29/2025
 * @license Licensed under the Apache License, Version 2.0
 * @description A brief description of the class functionality.
 * @notes <ul>
 * <li>Provide any additional notes or remarks here.</li>
 * </ul>
 * @since 6/29/2025
 */
public class Southbound {
    public static List<RubyMethodMetadata> rubyMethodMetadata = new ArrayList<>();

    public static List<String> extractDefBlocks(String fileContent) {
        List<String> defBlocks = new ArrayList<>();
        Matcher matcher = Pattern.compile("def\\s+\\w+[\\s\\S]*?^\\s*end\\s*$", Pattern.MULTILINE).matcher(fileContent);
        while (matcher.find()) {
            defBlocks.add(matcher.group());
        }
        return defBlocks;
    }

    public static void main(String[] args) throws IOException {
        Path basePath = Paths.get("D:\\Work\\Abacus\\transformation\\input");
        if (!Files.exists(basePath)) {
            System.err.println("Directory not found: " + basePath);
            return;
        }

        Files.walk(basePath)
                .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".rb"))
                .forEach(path -> {
                    try {
                        // Determine version from path like: input/southbound/v1/ads_client.rb
                        String version = extractVersionFromPath(path);
                        if (version == null) return;

                        // Read file content
                        String fileContent = Files.readString(path);

                        String backendVersion = extractBackendVersion(fileContent);

                        // Extract def blocks
                        List<String> defBlocks = extractDefBlocks(fileContent);


                        // Parse each block
                        for (String defBlock : defBlocks) {
                            RubyMethodMetadata metadata = RubyMethodParser.parseRubyMethod(defBlock, backendVersion);
                            metadata.southBoundVersion = version;
                            rubyMethodMetadata.add(metadata);
                            System.out.println("File: " + path.getFileName());
                            System.out.println(metadata);
                            System.out.println("------------");
                        }



                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        GenericCsvMapper.writeToCsv(rubyMethodMetadata, "southbound.csv");
    }

    private static String extractVersionFromPath(Path path) {
        // Expecting: input/southbound/v1/ads_client.rb
        Path parent = path.getParent();
        if (parent != null && parent.getFileName() != null) {
            return parent.getFileName().toString().toUpperCase(); // e.g., v1 -> V1
        }
        return null;
    }

    private static String extractBackendVersion(String fileContent) {
        Pattern versionPattern = Pattern.compile("BACKEND_VERSION\\s*=\\s*\"(.*?)\"");
        Matcher matcher = versionPattern.matcher(fileContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "V1"; // default
    }



}
