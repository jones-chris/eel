#!/bin/bash

EXCEL_FILE_NAME=$1
USER_NAME=$2
TRANSFORMER_NAME=$3
VERSION=$4

# validate excel file and create manifest.json file.
echo "Validating excel file and creating manifest.json file..."
java -cp ./engine/engine-core/target/engine-core-1.0-SNAPSHOT.jar io.eel.common.EelPackager "$EXCEL_FILE_NAME" "$USER_NAME" "$TRANSFORMER_NAME" "$VERSION"

# copy excel file and manifest into jar resources directory.
echo "Copying excel file and manifest into jar resources directory..."
jar uf ./engine/engine-core/target/engine-core-1.0-SNAPSHOT.jar "$EXCEL_FILE_NAME"
jar uf ./engine/engine-core/target/engine-core-1.0-SNAPSHOT.jar ./manifest.json

echo "Done!"