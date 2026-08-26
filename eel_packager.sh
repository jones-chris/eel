#!/bin/bash

EXCEL_FILE_NAME=$1
TRANSFORMER_NAME=$2

EXCEL_BASENAME=$(basename "$EXCEL_FILE_NAME")
EXCEL_DIR=$(dirname "$EXCEL_FILE_NAME")

if [ -z "$EXCEL_FILE_NAME" ] || [ -z "$TRANSFORMER_NAME" ]; then
    echo "Incorrect usage!  Correct usage: $0 <excel_file_path> <transformer_name> <version>"
    exit 1
fi

# If excel file name is not eel.xlsx, then throw error and exit.
if [ "$EXCEL_BASENAME" != "eel.xlsx" ]; then
    echo "Error: Only 'eel.xlsx' is allowed as the excel file name."
    exit 1
fi

# validate excel file and create manifest.json file.
echo "Validating excel file and creating manifest.json file..."
java -cp ./engine/engine-core/target/engine-core-1.0-SNAPSHOT.jar io.eel.common.EelPackager "$EXCEL_FILE_NAME" || exit 1

echo "Creating a new jar file..."
NEW_JAR_FILE_PATH="./$TRANSFORMER_NAME.jar"
cp ./engine/engine-core/target/engine-core-1.0-SNAPSHOT.jar "$NEW_JAR_FILE_PATH"

# copy excel file and manifest into jar resources directory.
echo "Copying excel file and manifest into jar resources directory..."

jar uf "$NEW_JAR_FILE_PATH" -C "$EXCEL_DIR" "$EXCEL_BASENAME"
jar uf "$NEW_JAR_FILE_PATH" ./manifest.json

echo "Create MCPB file for easy user installation into AI clients..."
MCPB_FILE_NAME="$TRANSFORMER_NAME.mcpb"

mkdir mcpb || exit 1
cp "$NEW_JAR_FILE_PATH" ./mcpb
cp ./mcpb_manifest.json ./mcpb/manifest.json # Notice that we rename the file here so it's what the MCPB format expects.
(cd mcpb && zip -r "../$MCPB_FILE_NAME" .)

echo "Deleting temporary files..."
rm ./manifest.json ./mcpb_manifest.json "$TRANSFORMER_NAME".jar
rm -rf ./mcpb

echo "Done!  Your MCPB file is ready at $MCPB_FILE_NAME.  Use this file to install your MCP server in the AI client of your choice, such as Claude Desktop."