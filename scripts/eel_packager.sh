#!/bin/bash

EXCEL_FILE_NAME=$1


# validate excel file.
java -cp ./core/target/core-1.0-SNAPSHOT.jar EelPackager

# Get manifest

# copy excel file into jar resources directory.

# copy jar with resources.
jar uf core-1.0-SNAPSHOT.jar "$EXCEL_FILE_NAME"
