package io.eel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opencsv.CSVReader;
import io.eel.common.WorkbookValidator;
import io.eel.common.model.WorkbookOutput;
import io.eel.service.WorkbookCalculationEngine;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.json.JsonMapper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MyMcpSyncServer {

    private static McpSyncServer syncServer;

    private static final WorkbookCalculationEngine engine;

    static {
        // Instantiate the workbook calculation engine (which will load the workbook and manifest)
        engine = new WorkbookCalculationEngine();
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
        syncServer.addTool(buildRunEngineTool());
    }

    private static McpServerFeatures.SyncToolSpecification buildManifestTool() {
        McpSchema.Tool manifestTool = McpSchema.Tool.builder()
                .name("get_manifest_of_" + engine.getManifest().name())
                .title("Get the manifest of " + engine.getManifest().name())
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

    private static McpServerFeatures.SyncToolSpecification buildRunEngineTool() {
        WorkbookValidator.Manifest manifest = engine.getManifest();

        // Build the input schema.
        Map<String, Object> inputJsonSchema = manifest.inputSheetsMetadata()
                .stream()
                .map(
                        inputSheetMetadata -> {
                            return Map.entry(
                                    inputSheetMetadata.name(),
                                    Map.of(
                                            "type", "string",
                                            "description", "The absolute file path to the input CSV file for sheet " + inputSheetMetadata.name()
                                    )
                            );
                        }
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Get a list containing each sheet's name.  This will be used to indicate that each sheet is a required input.
        List<String> inputSheetNames = manifest.inputSheetsMetadata()
                .stream()
                .map(WorkbookValidator.SheetMetadata::name)
                .toList();

        // Build the tool.
        McpSchema.Tool runWorkbookTool = McpSchema.Tool.builder()
                .name("run_workbook_engine_for_" + engine.getManifest().name())
                .title("Run the XLSX workbook engine for " + engine.getManifest().name())
                .description(
                        String.format(
                                """
                                Runs/executes the XLSX workbook engine for %s given the input CSV absolute file paths.
                                The response will contain the unique UUID of the workbook run/execution.  The client can
                                then subsequently call the "Get Workbook Run/Execution Result" tool with the UUID to
                                retrieve output data.
                                """,
                                engine.getManifest().name()
                        )
                )
                .inputSchema(
                        new McpSchema.JsonSchema(
                                "object",
                                inputJsonSchema,
                                inputSheetNames,
                                false,
                                Map.of(),
                                Map.of()
                        )
                )
                .outputSchema(
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "workbookExecutionId", Map.of("type", "string")
                                )
                        )
                )
                .meta(
                        Map.of("workbookExecutionId", "string")
                )
                .build();

        return new McpServerFeatures.SyncToolSpecification(
                runWorkbookTool,
                (mcpSyncServerExchange, callToRequest) -> {
                    for (WorkbookValidator.SheetMetadata inputSheetMetadata : manifest.inputSheetsMetadata()) {
                        Object filePath = callToRequest.arguments().get(inputSheetMetadata.name());
                        if (filePath == null) {
                            return new McpSchema.CallToolResult(
                                    List.of(new McpSchema.TextContent("Error: Missing required input sheet " + inputSheetMetadata.name())),
                                    true,
                                    new Object(),
                                    Map.of()
                            );
                        }

                        try {
                            InputStream inputStream = new FileInputStream(filePath.toString());
                            engine.withInput(inputSheetMetadata.name(), new CSVReader(new InputStreamReader(inputStream)));
                        } catch (FileNotFoundException e) {
                            return new McpSchema.CallToolResult(
                                    List.of(new McpSchema.TextContent("Error: File " + filePath + " not found for required input sheet " + inputSheetMetadata.name())),
                                    true,
                                    new Object(),
                                    Map.of()
                            );
                        }
                    }

                    try {
                        WorkbookOutput workbookOutput = engine.runWorkbook();
                        // todo:  change this later.
                        return McpSchema.CallToolResult.builder()
                                .isError(false)
                                .structuredContent(workbookOutput.getAllOutputs())
                                .build();
                    } catch (Exception e) {
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent("Error: There was an error running the workbook engine: " + e.getMessage())),
                                true,
                                new Object(),
                                Map.of()
                        );
                    }
                }
        );
    }

}