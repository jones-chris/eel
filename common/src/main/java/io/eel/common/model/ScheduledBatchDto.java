package io.eel.common.model;

/**
 *
 * @param type The {@link ScheduledBatchType} of the file.
 * @param storageLocation A {@link StorageLocation} of the zip file containing the CSV files for the related input sheets.
 */
public record ScheduledBatchDto(
        ScheduledBatchType type,
        StorageLocation storageLocation
) { }
