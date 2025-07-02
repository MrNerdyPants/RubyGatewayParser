package service;

/**
 * Represents the DescBlockExtractor class in the RubyGatewayParser project.
 *
 * @author Kashan Asim
 * @version 1.0
 * @project RubyGatewayParser
 * @module service
 * @class DescBlockExtractor
 * @lastModifiedBy Kashan.Asim
 * @lastModifiedDate 7/2/2025
 * @license Licensed under the Apache License, Version 2.0
 * @description A brief description of the class functionality.
 * @notes <ul>
 * <li>Provide any additional notes or remarks here.</li>
 * </ul>
 * @since 7/2/2025
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DescBlockExtractor {

    public static class DescBlock {
        private String descBlock;
        private String resource;

        public DescBlock(String descBlock, String resource) {
            this.descBlock = descBlock;
            this.resource = resource;
        }

        public String getDescBlock() {
            return descBlock;
        }

        public String getResource() {
            return resource;
        }

        @Override
        public String toString() {
            return String.format("DescBlock{resource='%s'}", resource);
        }
    }


    public static class DescBlocksContents{
        private List<DescBlock> descBlocks;
        private String wholeContent;

        public String getWholeContent(){
            return wholeContent;
        }

        public List<DescBlock> getDescBlocks(){
            return descBlocks;
        }
    }

    public static DescBlocksContents extractDescBlocks(BufferedReader reader) throws IOException {
        DescBlocksContents blocksContents = new DescBlocksContents();
        List<DescBlock> descBlocks = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        String line;

        // Read all lines
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }

        blocksContents.wholeContent = String.join("", lines);

        Stack<String> resourceStack = new Stack<>();
        int i = 0;

        while (i < lines.size()) {
            line = lines.get(i).trim();

            // Track resource blocks
            if (line.startsWith("resource :")) {
                String resourceName = extractResourceName(line);
                resourceStack.push(resourceName);
                i++;
                continue;
            }

            // Track end of resource blocks
            if (line.equals("end") && !resourceStack.isEmpty()) {
                String currentLine = lines.get(i);
                int indentLevel = getIndentationLevel(currentLine);

                // Simple heuristic: if 'end' is at low indentation, it likely closes a resource
                if (indentLevel <= 4) { // Adjust based on your indentation style
                    resourceStack.pop();
                }
                i++;
                continue;
            }

            // Found a desc block - extract complete block including params and HTTP method
            if (line.startsWith("desc ")) {
                String currentResource = buildResourcePath(resourceStack);
                CompleteBlockResult blockResult = extractCompleteDescBlock(lines, i);

                descBlocks.add(new DescBlock(
                        blockResult.content,
                        currentResource.isEmpty() ? null : currentResource
                ));

                // Move to after this complete block
                i = blockResult.endIndex + 1;
            } else {
                i++;
            }
        }

        blocksContents.descBlocks = descBlocks;
        return blocksContents;
    }

    private static class CompleteBlockResult {
        String content;
        int endIndex;

        CompleteBlockResult(String content, int endIndex) {
            this.content = content;
            this.endIndex = endIndex;
        }
    }

    private static CompleteBlockResult extractCompleteDescBlock(List<String> lines, int startIndex) {
        StringBuilder blockContent = new StringBuilder();
        int i = startIndex;

        // 1. First, capture the desc declaration
        blockContent.append(lines.get(i)).append("\n");
        i++;

        // Capture the desc block (with braces)
        int braceCount = 0;
        boolean foundOpenBrace = false;

        while (i < lines.size()) {
            String line = lines.get(i);
            blockContent.append(line).append("\n");

            // Count braces
            for (char c : line.toCharArray()) {
                if (c == '{') {
                    braceCount++;
                    foundOpenBrace = true;
                }
                if (c == '}') {
                    braceCount--;
                }
            }

            // If we've found opening brace and braces are balanced, desc block is done
            if (foundOpenBrace && braceCount == 0) {
                i++;
                break;
            }

            i++;
        }

        // 2. Now capture params block (if present)
        while (i < lines.size()) {
            String line = lines.get(i).trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                blockContent.append(lines.get(i)).append("\n");
                i++;
                continue;
            }

            // Found params block
            if (line.startsWith("params do")) {
                blockContent.append(lines.get(i)).append("\n");
                i++;

                // Capture entire params block until its 'end'
                int paramsIndentLevel = getIndentationLevel(lines.get(i - 1));
                while (i < lines.size()) {
                    String paramsLine = lines.get(i);
                    blockContent.append(paramsLine).append("\n");

                    if (paramsLine.trim().equals("end") &&
                            getIndentationLevel(paramsLine) <= paramsIndentLevel + 2) {
                        i++;
                        break;
                    }
                    i++;
                }
                continue;
            }

            // 3. Found HTTP method - this completes our block
            if (isHttpMethod(line)) {
                blockContent.append(lines.get(i)).append("\n");
                i++;

                // Capture the method body until its 'end'
                int methodIndentLevel = getIndentationLevel(lines.get(i - 1));
                while (i < lines.size()) {
                    String methodLine = lines.get(i);
                    blockContent.append(methodLine).append("\n");

                    if (methodLine.trim().equals("end") &&
                            getIndentationLevel(methodLine) <= methodIndentLevel + 2) {
                        // This 'end' closes the HTTP method block
                        break;
                    }
                    i++;
                }
                break;
            }

            // If we hit another desc or resource, we're done
            if (line.startsWith("desc ") || line.startsWith("resource :")) {
                i--; // Step back so we don't skip the next desc/resource
                break;
            }

            // Add any other lines that might be between params and method
            blockContent.append(lines.get(i)).append("\n");
            i++;
        }

        return new CompleteBlockResult(blockContent.toString().trim(), i);
    }

    private static boolean isHttpMethod(String line) {
        return line.matches("^(get|post|put|delete|patch)\\b.*");
    }

    private static String extractResourceName(String line) {
        Pattern pattern = Pattern.compile("resource\\s+:([a-zA-Z_]+)");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : "unknown";
    }

    private static String buildResourcePath(Stack<String> resourceStack) {
        if (resourceStack.isEmpty()) {
            return "";
        }
        return String.join("/", resourceStack);
    }

    private static int getIndentationLevel(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else if (c == '\t') count += 4;
            else break;
        }
        return count;
    }

    // Usage example
    public static void main(String[] args) {
        try {
            String sampleContent = """
                    resource :activation do

                      desc 'Request Customer', {
                        headers: {
                          'Session-Id' => {
                            description: 'User session ID',
                            required: true
                          }
                        }
                      }
                      post :request_customer do
                        @client.request_customer
                      end

                      desc 'Request Family', {
                        headers: {
                          'Session-Id' => {
                            description: 'User session ID',
                            required: true
                          }
                        }
                      }
                      params do
                        requires :number, type: String
                      end
                      post :request_family do
                        @client.request_family(params[:number])
                      end

                      desc 'modules', {
                        headers: {
                          'Session-Id' => {
                            description: 'User session ID',
                            required: true
                          }
                        }
                      }
                      params do
                        optional :include_customer_data, type: Boolean, default: true
                      end
                      get :modules do
                        @client.modules(params[:include_customer_data])[:modules]
                      end

                      resource :absher_token do
                        desc 'GET vtoken', {
                          headers: {
                            'Session-Id' => {
                              description: 'User session ID',
                              required: true
                            }
                          }
                        }
                        get do
                          @client.get_token
                        end

                        desc 'POST token', {
                          headers: {
                            'Session-Id' => {
                              description: 'User session ID',
                              required: true
                            }
                          }
                        }
                        params do
                          requires :token, type: String
                          requires :creation_date, type: DateTime
                        end
                        post do
                          @client.post_token(params[:token], params[:creation_date])
                        end
                      end

                    end
                    """;

            BufferedReader reader = new BufferedReader(new StringReader(sampleContent));
            DescBlocksContents blocksContents = extractDescBlocks(reader);

            List<DescBlock> descBlocks = blocksContents.descBlocks;


            System.out.println("Found " + descBlocks.size() + " complete desc blocks:");
            System.out.println();

            for (int i = 0; i < descBlocks.size(); i++) {
                DescBlock block = descBlocks.get(i);
                System.out.println("=== Complete Desc Block " + (i + 1) + " ===");
                System.out.println("Resource: " + (block.getResource() != null ? block.getResource() : "null"));
                System.out.println("Complete Block Content:");
                System.out.println(block.getDescBlock());
                System.out.println();
                System.out.println("----------------------------------------");
                System.out.println();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

//public class DescBlockExtractor {
//
//    public static class DescBlock {
//        private String descBlock;
//        private String resource;
//
//        public DescBlock(String descBlock, String resource) {
//            this.descBlock = descBlock;
//            this.resource = resource;
//        }
//
//        public String getDescBlock() { return descBlock; }
//        public String getResource() { return resource; }
//
//        @Override
//        public String toString() {
//            return String.format("DescBlock{descBlock='%s', resource='%s'}",
//                    descBlock.substring(0, Math.min(50, descBlock.length())) + "...", resource);
//        }
//    }
//
//    public static List<DescBlock> extractDescBlocks(BufferedReader reader) throws IOException {
//        List<DescBlock> descBlocks = new ArrayList<>();
//        List<String> lines = new ArrayList<>();
//        String line;
//
//        // Read all lines
//        while ((line = reader.readLine()) != null) {
//            lines.add(line);
//        }
//
//        Stack<String> resourceStack = new Stack<>();
//        int i = 0;
//
//        while (i < lines.size()) {
//            line = lines.get(i).trim();
//
//            // Track resource blocks
//            if (line.startsWith("resource :")) {
//                String resourceName = extractResourceName(line);
//                resourceStack.push(resourceName);
//                i++;
//                continue;
//            }
//
//            // Track end of resource blocks (simplified - assumes proper nesting)
//            if (line.equals("end") && !resourceStack.isEmpty()) {
//                String currentLine = lines.get(i);
//                int indentLevel = getIndentationLevel(currentLine);
//
//                // Simple heuristic: if 'end' is at low indentation, it likely closes a resource
//                if (indentLevel <= 4) { // Adjust based on your indentation style
//                    resourceStack.pop();
//                }
//                i++;
//                continue;
//            }
//
//            // Found a desc block
//            if (line.startsWith("desc ")) {
//                String currentResource = buildResourcePath(resourceStack);
//                String descContent = extractDescBlock(lines, i);
//
//                descBlocks.add(new DescBlock(
//                        descContent,
//                        currentResource.isEmpty() ? null : currentResource
//                ));
//
//                // Skip to after this desc block
//                i = findEndOfDescBlock(lines, i) + 1;
//            } else {
//                i++;
//            }
//        }
//
//        return descBlocks;
//    }
//
//    private static String extractDescBlock(List<String> lines, int startIndex) {
//        StringBuilder descContent = new StringBuilder();
//        int i = startIndex;
//
//        // Add the desc line
//        descContent.append(lines.get(i)).append("\n");
//        i++;
//
//        // Find the end of the desc block by tracking braces
//        int braceCount = 0;
//        boolean foundOpenBrace = false;
//
//        while (i < lines.size()) {
//            String line = lines.get(i);
//            descContent.append(line).append("\n");
//
//            // Count braces
//            for (char c : line.toCharArray()) {
//                if (c == '{') {
//                    braceCount++;
//                    foundOpenBrace = true;
//                }
//                if (c == '}') {
//                    braceCount--;
//                }
//            }
//
//            // If we've found opening brace and braces are balanced, we're done
//            if (foundOpenBrace && braceCount == 0) {
//                break;
//            }
//
//            i++;
//        }
//
//        return descContent.toString().trim();
//    }
//
//    private static int findEndOfDescBlock(List<String> lines, int startIndex) {
//        int i = startIndex + 1;
//        int braceCount = 0;
//        boolean foundOpenBrace = false;
//
//        while (i < lines.size()) {
//            String line = lines.get(i);
//
//            for (char c : line.toCharArray()) {
//                if (c == '{') {
//                    braceCount++;
//                    foundOpenBrace = true;
//                }
//                if (c == '}') {
//                    braceCount--;
//                }
//            }
//
//            if (foundOpenBrace && braceCount == 0) {
//                return i;
//            }
//
//            i++;
//        }
//
//        return i - 1;
//    }
//
//    private static String extractResourceName(String line) {
//        Pattern pattern = Pattern.compile("resource\\s+:([a-zA-Z_]+)");
//        Matcher matcher = pattern.matcher(line);
//        return matcher.find() ? matcher.group(1) : "unknown";
//    }
//
//    private static String buildResourcePath(Stack<String> resourceStack) {
//        if (resourceStack.isEmpty()) {
//            return "";
//        }
//        return String.join("/", resourceStack);
//    }
//
//    private static int getIndentationLevel(String line) {
//        int count = 0;
//        for (char c : line.toCharArray()) {
//            if (c == ' ') count++;
//            else if (c == '\t') count += 4;
//            else break;
//        }
//        return count;
//    }
//
//    // Usage example
//    public static void main(String[] args) {
//        try {
//            String sampleContent = """
//                module Virgin
//                  module V1
//                    class Activation < Base
//                      before { auth!(Virgin::API::V1::Activation) }
//
//                      resource :activation do
//
//                        desc 'Request Customer', {
//                          headers: {
//                            'Session-Id' => {
//                              description: 'User session ID',
//                              required: true
//                            }
//                          }
//                        }
//                        post :request_customer do
//                          @client.request_customer
//                        end
//
//                        desc 'Request Family', {
//                          headers: {
//                            'Session-Id' => {
//                              description: 'User session ID',
//                              required: true
//                            }
//                          }
//                        }
//                        params do
//                          requires :number, type: String
//                        end
//                        post :request_family do
//                          @client.request_family(params[:number])
//                        end
//
//                        resource :absher_token do
//                          desc 'GET vtoken', {
//                            headers: {
//                              'Session-Id' => {
//                                description: 'User session ID',
//                                required: true
//                              }
//                            }
//                          }
//                          get do
//                            @client.get_token
//                          end
//
//                          desc 'POST token', {
//                            headers: {
//                              'Session-Id' => {
//                                description: 'User session ID',
//                                required: true
//                              }
//                            }
//                          }
//                          params do
//                            requires :token, type: String
//                            requires :creation_date, type: DateTime
//                          end
//                          post do
//                            @client.post_token(params[:token], params[:creation_date])
//                          end
//                        end
//
//                      end
//                    end
//                  end
//                end
//                """;
//
//            BufferedReader reader = new BufferedReader(new StringReader(sampleContent));
//            List<DescBlock> descBlocks = extractDescBlocks(reader);
//
//            System.out.println("Found " + descBlocks.size() + " desc blocks:");
//            System.out.println();
//
//            for (int i = 0; i < descBlocks.size(); i++) {
//                DescBlock block = descBlocks.get(i);
//                System.out.println("=== Desc Block " + (i+1) + " ===");
//                System.out.println("Resource: " + (block.getResource() != null ? block.getResource() : "null"));
//                System.out.println("Desc Block:");
//                System.out.println(block.getDescBlock());
//                System.out.println();
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
