package io.eel;

import com.github.victools.jsonschema.generator.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.eel.common.WorkbookValidator;
import io.eel.service.WorkbookCalculationEngine;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MyMcpSyncServer {

    private static McpSyncServer syncServer;

    private static final WorkbookCalculationEngine engine;

    private static final ObjectNode manifestJsonSchema;

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static {
        // Instantiate the workbook calculation engine (which will load the workbook and manifest)
        engine = new WorkbookCalculationEngine();

        // Generate JSON Schema for the Manifest class.
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON);
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        manifestJsonSchema = generator.generateSchema(WorkbookValidator.Manifest.class);
    }

    public static void main(String[] args) throws Exception {
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new JsonMapper());

        // 1. Setup Transport (Keep it simple)
        StdioServerTransportProvider transport = new StdioServerTransportProvider(jsonMapper);


        // 2. Build the Server (No start() needed after this)
        syncServer = McpServer.sync(transport)
                .serverInfo("EEL-MCP-Server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .build();

        WorkbookValidator.Manifest manifest = engine.getManifest();
        List<WorkbookValidator.SheetMetadata> inputMetadata = manifest.inputSheetsMetadata();
        String sheetName = inputMetadata.getFirst().name();

        // 4. Register a Tool
        McpSchema.Tool calculatorTool = new McpSchema.Tool(
                "calculator",
                "Basic calculator",
                "Performs basic arithmetic " + sheetName,
                new McpSchema.JsonSchema(
                        "object",
                        Map.of(
                                sheetName, Map.of(
                                        "type", "string",
                                        "description", "The " + sheetName + " to perform: add, subtract, multiply, divide"
                                ),
                                "a", Map.of(
                                        "type", "number",
                                        "description", "The first operand"
                                ),
                                "b", Map.of(
                                        "type", "number",
                                        "description", "The second operand"
                                )
                        ),
                        List.of(sheetName, "a", "b"),
                        false,
                        Map.of(),
                        Map.of()
                ),
                null,
//                Map.of(),
                null,
//                new McpSchema.ToolAnnotations(
//                        "",
//                        false,
//                        false,
//                        false,
//                        false,
//                        false
//                ),
                Map.of(
                        sheetName, "string",
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
        syncServer.addTool(buildManifestTool());
    }

    private static McpServerFeatures.SyncToolSpecification buildManifestTool() {
        McpSchema.Tool manifestTool = McpSchema.Tool.builder()
                .name("Get MicroTransformer Manifest for " + engine.getManifest().name())
                .title("Get the manifest of the MicroTransformer " + engine.getManifest().name())
                .description(
                        String.format(
                                """
                                Gets the manifest of the MicroTransformer %s, which includes metadata about the input
                                sheets and output sheets
                                """,
                                engine.getManifest().name()
                        )
                )
                .inputSchema(new McpSchema.JsonSchema(
                        "object",
                        Map.of(),
                        List.of(),
                        false,
                        Map.of(),
                        Map.of()
                ))
                .outputSchema(
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "manifest", Map.of("type", "object")
                                )
                        )
                )
                .meta(
                        Map.of("manifest", "object")
                )
                .build();

        return new McpServerFeatures.SyncToolSpecification(
                manifestTool,
                (mcpSyncServerExchange, callToRequest) -> McpSchema.CallToolResult.builder()
                        .isError(false)
                        .structuredContent(engine.getManifest())
                        .build()
        );
    }
}