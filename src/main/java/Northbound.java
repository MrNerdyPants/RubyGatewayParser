import model.ApiMetadata;
import service.DescBlockParser;
import service.GenericCsvMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Represents the Main class in the RubyTransformation project.
 *
 * @author Kashan Asim
 * @version 1.0
 * @project RubyTransformation
 * @module PACKAGE_NAME
 * @class Main
 * @lastModifiedBy Kashan.Asim
 * @lastModifiedDate 6/27/2025
 * @license Licensed under the Apache License, Version 2.0
 * @description A brief description of the class functionality.
 * @notes <ul>
 * <li>Provide any additional notes or remarks here.</li>
 * </ul>
 * @since 6/27/2025
 */
public class Northbound {
    public static List<ApiMetadata> apiMetadata = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        //Path rootDir = Paths.get("D:\\Work\\Abacus\\transformation\\input");
        Path rootDir = Paths.get("C:\\Abacus projects\\Ruby-Code-Input");

        //C:\Abacus projects\Ruby-Code-Input
        // Traverse both northbound and southbound
        traverseAndExtract(rootDir.resolve("northbound"));

        GenericCsvMapper.writeToCsv(apiMetadata, "northbound.csv");
//        traverseAndExtract(rootDir.resolve("southbound"));
    }

    private static void traverseAndExtract(Path baseDir) throws IOException {
        if (!Files.exists(baseDir)) return;

        try (Stream<Path> paths = Files.walk(baseDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".rb"))
                    .forEach(Northbound::processFile);
        }
    }


    public static List<String> extractDefBlocks(String fileContent) {
        List<String> defBlocks = new ArrayList<>();
        Matcher matcher = Pattern.compile("def\\s+\\w+[\\s\\S]*?^\\s*end\\s*$", Pattern.MULTILINE).matcher(fileContent);
        while (matcher.find()) {
            defBlocks.add(matcher.group());
        }
        return defBlocks;
    }

    public static List<String> extractDefBlocks2(String fileContent) {
        List<String> blocks = new ArrayList<>();
        List<String> currentBlock = new ArrayList<>();
        boolean inBlock = false;
        int openDoCount = 0;

        String[] lines = fileContent.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("desc") && !inBlock) {
                inBlock = true;
                currentBlock = new ArrayList<>();
            }

            if (inBlock) {
                currentBlock.add(line);

                if (trimmed.matches(".*\\bdo\\b.*")) openDoCount++;
                if (trimmed.equals("end")) openDoCount--;

                if (openDoCount <= 0 && trimmed.matches("^(get|post|put|delete)\\b.*")) {
                    // These usually end after `get do` ... `end`
                    blocks.add(String.join("\n", currentBlock));
                    inBlock = false;
                    openDoCount = 0;
                }
            }
        }

        // Add the last block if the file ended before closing
        if (inBlock && !currentBlock.isEmpty()) {
            blocks.add(String.join("\n", currentBlock));
        }

        return blocks;
    }


    private static void processFile(Path filePath) {
        System.out.println("\n--- Extracting from: " + filePath + " ---");

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            StringBuilder wholeFileContent = new StringBuilder();
            String parentDir = filePath.getParent().getFileName().toString();
            String fileBaseName = filePath.getFileName().toString();
            fileBaseName = fileBaseName.replace(".rb", "");

            String line;
            boolean capturing = false;
            StringBuilder currentBlock = new StringBuilder();
            List<String> blocks = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                wholeFileContent.append(line).append("\n");
                if (line.trim().startsWith("desc ")) {
                    // New desc block begins
                    if (capturing && currentBlock.length() > 0) {
                        blocks.add(currentBlock.toString());
                        currentBlock.setLength(0); // reset
                    }
                    capturing = true;
                }

                if (capturing) {
                    currentBlock.append(line).append("\n");
                }
            }

            // Add the last block if file ends without a new "desc"
            if (capturing && currentBlock.length() > 0) {
                blocks.add(currentBlock.toString());
            }


            // Print blocks (or you can save them somewhere)
            for (int i = 0; i < blocks.size(); i++) {

                ApiMetadata meta = DescBlockParser.parseDescBlock(blocks.get(i), fileBaseName, wholeFileContent.toString());
                meta.northboundVersion = parentDir;

                apiMetadata.add(meta);

                System.out.println("API Name: " + parentDir + " " + meta.apiName);
                System.out.println("Headers: " + meta.headers);
                System.out.println("HTTP Method: " + meta.httpMethod);
                System.out.println("Endpoint: " + meta.endpoint);
                System.out.println("Request Body (JSON):\n" + meta.jsonBody);
                System.out.println("southbound version: " + meta.southboundVersion);
                System.out.println("southbound method: " + meta.southboundMethod);

                System.out.println("\n");
//                System.out.println("Block #" + (i + 1) + ":\n" + blocks.get(i));
            }




        } catch (IOException e) {
            System.err.println("Failed to read file: " + filePath + " - " + e.getMessage());
        }
    }
}
