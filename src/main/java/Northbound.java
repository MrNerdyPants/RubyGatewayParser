import model.ApiMetadata;
import service.DescBlockExtractor;
import service.DescBlockParser;
import service.GenericCsvMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static service.DescBlockExtractor.extractDescBlocks;

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
        Path rootDir = Paths.get("D:\\Work\\Abacus\\transformation\\input");

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


    private static void processFile(Path filePath) {
        System.out.println("\n--- Extracting from: " + filePath + " ---");

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            StringBuilder wholeFileContent = new StringBuilder();
            String parentDir = filePath.getParent().getFileName().toString();
            String fileBaseName = filePath.getFileName().toString();
            fileBaseName = fileBaseName.replace(".rb", "");

//            String line;
//            boolean capturing = false;
//            StringBuilder currentBlock = new StringBuilder();
//            List<String> blocks = new ArrayList<>();
//
//            while ((line = reader.readLine()) != null) {
//                wholeFileContent.append(line).append("\n");
//                if (line.trim().startsWith("desc ")) {
//                    // New desc block begins
//                    if (capturing && currentBlock.length() > 0) {
//                        blocks.add(currentBlock.toString());
//                        currentBlock.setLength(0); // reset
//                    }
//                    capturing = true;
//                }
//
//                if (capturing) {
//                    currentBlock.append(line).append("\n");
//                }
//            }
//
//            // Add the last block if file ends without a new "desc"
//            if (capturing && currentBlock.length() > 0) {
//                blocks.add(currentBlock.toString());
//            }

            DescBlockExtractor.DescBlocksContents blocksContents = extractDescBlocks(reader);
            List<DescBlockExtractor.DescBlock> blocks = blocksContents.getDescBlocks();

            // Print blocks (or you can save them somewhere)
            for (int i = 0; i < blocks.size(); i++) {

                ApiMetadata meta = DescBlockParser.parseDescBlock(blocks.get(i), fileBaseName, blocksContents.getWholeContent());
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
