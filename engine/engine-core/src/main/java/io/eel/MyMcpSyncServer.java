package io.eel;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

public class MyMcpSyncServer {

    public static void main(String[] args) {
        // 1. Initialize the JSON mapper for the transport
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new JsonMapper());

        // 2. Initialize the transport (using STDIO for local inter-process comms)
        StdioServerTransportProvider transport = new StdioServerTransportProvider(jsonMapper);

        // 3. Build the Synchronous Server with the transport
        McpSyncServer syncServer = McpServer.sync(transport)
                .serverInfo("EEL-MCP-Server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true) // Enable tool capability
                        .logging()   // Enable logging support
                        .build())
                .build();

        // 4. Register a Tool
        McpSchema.Tool calculatorTool = new McpSchema.Tool(
                "calculator",
                "Basic calculator",
                "Performs basic arithmetic operations",
                new McpSchema.JsonSchema(
                        "",
                        Map.of(),
                        List.of(),
                        false,
                        Map.of(),
                        Map.of()
                ),
                Map.of(),
                new McpSchema.ToolAnnotations(
                        "",
                        false,
                        false,
                        false,
                        false,
                        false
                ),
                Map.of(
                        "operation", "string",
                        "a", "number",
                        "b", "number"
                )
        );

        McpServerFeatures.SyncToolSpecification syncToolRegistration = new McpServerFeatures.SyncToolSpecification(
                calculatorTool,
                (mcpSyncServerExchange, callToRequest) -> {
                    String operation = (String) callToRequest.arguments().get("operation");
                    Number a = (Number) callToRequest.arguments().get("a");
                    Number b = (Number) callToRequest.arguments().get("b");

                    double result = 0;
                    if ("add".equals(operation)) {
                        result = a.doubleValue() + b.doubleValue();
                    } else if ("subtract".equals(operation)) {
                        result = a.doubleValue() - b.doubleValue();
                    } else if ("multiply".equals(operation)) {
                        result = a.doubleValue() * b.doubleValue();
                    } else if ("divide".equals(operation)) {
                        if (b.doubleValue() != 0) {
                            result = a.doubleValue() / b.doubleValue();
                        } else {
                            return new McpSchema.CallToolResult(
                                    List.of(new McpSchema.TextContent("Error: Division by zero")),
                                    true,
                                    new Object(),
                                    Map.of()
                            );
                        }
                    }

                    return new McpSchema.CallToolResult(
                            List.of(new McpSchema.TextContent("Result: " + result)),
                            false,
                            new Object(),
                            Map.of()
                    );
                }
        );

        // Register the handler
        syncServer.addTool(syncToolRegistration);

        // 5. Start the server (this blocks the thread)
        System.err.println("Starting EEL MCP Sync Server on STDIO...");
//        try {
//            transport.runLoop(syncServer);
//        } catch (Exception e) {
//            System.err.println("Server error: " + e.getMessage());
//            e.printStackTrace();
//        }

        // Keep the application running
        Runtime.getRuntime().addShutdownHook(new Thread(syncServer::close));
    }
}