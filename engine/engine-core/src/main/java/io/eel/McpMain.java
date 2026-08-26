package io.eel;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class McpMain {

    private static McpSyncServer syncServer;

    private static WorkbookCalculationEngine engine;

    static {
        // Instantiate the workbook calculation engine (which will load the workbook and manifest)
        engine = new WorkbookCalculationEngine();
    }

    public static void main(String[] args) throws Exception {
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new JsonMapper());

        // Set up the STDIO transport for a local MCP server.
        StdioServerTransportProvider transport = new StdioServerTransportProvider(jsonMapper);


        // Build the MCP server.
        syncServer = McpServer.sync(transport)
                .serverInfo("ArcWeldr-MCP-Server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .build();

        // Register the MCP tools.
        syncServer.addTool(buildHealthcheckTool());
        syncServer.addTool(buildManifestTool());
        syncServer.addTool(buildRunEngineTool());
    }

    private static McpServerFeatures.SyncToolSpecification buildHealthcheckTool() {
        McpSchema.Tool healthCheckTool = new McpSchema.Tool(
                "health_check",
                "Health Check",
                "This is a basic health check tool that ensures the MCP server is running",
                new McpSchema.JsonSchema(
                        "object",
                        null,
                        null,
                        false,
                        Map.of(),
                        Map.of()
                ),
                null,
                null,
                Map.of()
        );

        return new McpServerFeatures.SyncToolSpecification(
                healthCheckTool,
                (mcpSyncServerExchange, callToRequest) -> McpSchema.CallToolResult.builder()
                        .isError(false)
                        .structuredContent(
                                Map.of("isHealthy", true)
                        )
                        .build()
        );
    }

    private static McpServerFeatures.SyncToolSpecification buildManifestTool() {
        String mcpFriendlyName = engine.getManifest().name().toLowerCase().replace(" ", "_");

        McpSchema.Tool manifestTool = McpSchema.Tool.builder()
                .name("get_manifest_of_" + mcpFriendlyName)
                .title("Get the manifest of " + engine.getManifest().name())
                .description(
                        "Returns the input/output schema (sheet names, field names, types) for the Validate Transaction Categories workbook, without running it."
                )
                .inputSchema(
                        new McpSchema.JsonSchema(
                                "object",
                                Map.of(),
                                List.of(),
                                false,
                                Map.of(),
                                Map.of()
                        )
                )
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
                        inputSheetMetadata -> Map.entry(
                                inputSheetMetadata.name(),
                                Map.of(
                                        "type", "string",
                                        "description", "The absolute file path to the input CSV file for sheet " + inputSheetMetadata.name()
                                )
                        )
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        inputJsonSchema.put(
                "debug_mode_enabled",
                Map.of(
                        "type", "string",
                        "description", """
                                true if the workbook should be written to a storage location and the file path returned
                                in the response.  This can be useful if the user wants to open or debug the XSLX file
                                after calculation.  Otherwise, false.
                            """
                )
        );

        // Get a list containing each sheet's name.  This will be used to indicate that each sheet is a required input.
        List<String> inputSheetNames = manifest.inputSheetsMetadata()
                .stream()
                .map(WorkbookValidator.SheetMetadata::name)
                .toList();

        // Build the tool.
        // todo:  consolidate this duplicated logic in a method/class.
        String mcpFriendlyName = engine.getManifest().name().toLowerCase().replace(" ", "_");

        McpSchema.Tool runWorkbookTool = McpSchema.Tool.builder()
                .name("run_workbook_engine_for_" + mcpFriendlyName)
                .title("Run the XLSX workbook engine for " + engine.getManifest().name())
                .description(
                        String.format(
                                """
                                Runs/executes the XLSX workbook engine for %s given the input CSV absolute file paths.
                                Refer to the get_manifest tool in this MCP server for the input and output fields and data 
                                types.
                                
                                The description of the workbook is as follows:
                                %s
                                """,
                                engine.getManifest().name(),
                                engine.getManifest().description()
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

                    try {
                        manifest.inputSheetsMetadata()
                                .parallelStream() // Write the data to the workbook in parallel so that it's faster.
                                .forEach(
                                        inputSheetMetadata -> {
                                            Object filePath = callToRequest.arguments().get(inputSheetMetadata.name());
                                            if (filePath == null) {
                                                throw new IllegalArgumentException("Error: Missing required input sheet " + inputSheetMetadata.name());
                                            }

                                            try {
                                                InputStream inputStream = new FileInputStream(filePath.toString());

                                                CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(inputStream))
                                                        .withSkipLines(1) // Skip the required header row.
                                                        .build();

                                                engine.withInput(inputSheetMetadata.name(), csvReader);
                                            } catch (FileNotFoundException e) {
                                                throw new RuntimeException("Error: File " + filePath + " not found for required input sheet " + inputSheetMetadata.name());
                                            }
                                        }
                                );
                    } catch (Throwable t) {
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(t.getMessage())),
                                true,
                                null,
                                Map.of()
                        );
                    }

                    try {
                        WorkbookOutput workbookOutput = engine.runWorkbook();
                        Map<String, Object> resultContent = new HashMap<>();
                        resultContent.put("output", workbookOutput.getAllOutputs());
                        if (workbookOutput.getStorageLocation().isPresent()) {
                            resultContent.put("storageLocation", workbookOutput.getStorageLocation().get());
                        }

                        return McpSchema.CallToolResult.builder()
                                .isError(false)
                                .structuredContent(resultContent)
                                .build();
                    } catch (Exception e) {
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent("Error: There was an error running the workbook engine: " + stringifyExceptionStackTrace(e))),
                                true,
                                new Object(),
                                Map.of()
                        );
                    }
                }
        );
    }

    private static String stringifyExceptionStackTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.toString()).append("\n");

        for (StackTraceElement element : t.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }

        return sb.toString();
    }

}