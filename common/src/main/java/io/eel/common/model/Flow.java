package io.eel.common.model;

import java.util.UUID;

public record Flow(
    UUID id,
    String author,
    String landingBucketIdentifier,
    String outputBucketIdentifier
) { }
