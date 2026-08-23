#!/bin/bash

EXCEL_FILE_NAME=$1
#USER_NAME=$2
#TRANSFORMER_NAME=$3
VERSION=$2

EXCEL_BASENAME=$(basename "$EXCEL_FILE_NAME")
EXCEL_DIR=$(dirname "$EXCEL_FILE_NAME")

if [ -z "$EXCEL_FILE_NAME" ] || [ -z "$VERSION" ]; then
    echo "Incorrect usage!  Correct usage: $0 <excel_file_path> <version>"
    exit 1
fi

# If excel file name is not eel.xlsx, then throw error and exit.
if [ "$EXCEL_BASENAME" != "eel.xlsx" ]; then
    echo "Error: Only 'eel.xlsx' is allowed as the excel file name."
    exit 1
fi

# validate excel file and create manifest.json file.
echo "Validating excel file and creating manifest.json file..."
java -cp ./engine/engine-core/target/engine-core-1.0-SNAPSHOT.jar io.eel.common.EelPackager "$EXCEL_FILE_NAME" "$VERSION" || exit 1

echo "Creating a new jar file..."
NEW_JAR_FILE_PATH="./$TRANSFORMER_NAME-v$VERSION.jar"
cp ./engine/engine-core/target/engine-core-1.0-SNAPSHOT.jar "$NEW_JAR_FILE_PATH"

# copy excel file and manifest into jar resources directory.
echo "Copying excel file and manifest into jar resources directory..."

jar uf "$NEW_JAR_FILE_PATH" -C "$EXCEL_DIR" "$EXCEL_BASENAME"
jar uf "$NEW_JAR_FILE_PATH" ./manifest.json

echo "Done!"