#!/bin/bash

EXCEL_FILE_NAME=$1
USER_NAME=$2
TRANSFORMER_NAME=$3
VERSION=$4

# validate excel file.
java -cp ./core/target/core-1.0-SNAPSHOT.jar io.eel.packager.EelPackager "$EXCEL_FILE_NAME" "$USER_NAME" "$TRANSFORMER_NAME" "$VERSION"

# copy excel file and manifest into jar resources directory.
jar uf core-1.0-SNAPSHOT.jar "$EXCEL_FILE_NAME"
jar uf core-1.0-SNAPSHOT.jar ./eel_manifest.json

# copy jar with resources.
