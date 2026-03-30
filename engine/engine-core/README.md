## Engine Core
Engine Core contains cloud-agnostic code that wraps around a XLSX workbook.  It allows a user to programmatically inspect the 
XLSX workbook's metadata, read and write to cells, and perform calculations.

Engine Core also contains a MCP server/API that allows AI MCP clients to interact with this wrapper.  

### Testing the MCP Server/API
To test the MCP server/API, you can use MCP Inspector.  Do the following:

1. Build the engine-core project by running `mvn clean install` in the engine-core directory.
2. Start the MCP Inspector by running `npx @modelcontextprotocol/inspector --config ./engine-core/src/test/resources/mcp_server_config.json eel`
3. Copy the link in the output of the previous command and open it in your browser.  You should see the MCP Inspector interface.
4. In the MCP Inspector interface, click on the "Connect" button to connect to the MCP server.
5. Once connected, click `List Tools` to see the MCP server's available tools.
6. Click on any tool to see its details, including its name, description, and parameters.
7. Choose a tool, fill out the parameters, and click the `Run Tool` button.  There should be a new item in the `History` 
panel that shows a successful response.