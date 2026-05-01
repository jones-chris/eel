package io.eel.common.model;

public enum TransformationExtractionType {

    MANIFEST("manifests/"),
    ARTIFACT("artifacts/");

    private final String prefix;

    TransformationExtractionType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return this.prefix;
    }

}