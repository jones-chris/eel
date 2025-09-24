package io.eel.common.model;

import io.eel.common.WorkbookValidator;

import java.util.Map;

public record ScheduledBatchConfiguration(

        /*
         * The CRON expression that the flow runs on.
         */
        String cronExpression,

        /*
         * The queries that are executed for each of the manifest's input sheets.
         */
        Map<String, Query> sheetQueries,

        /*
         * The input bucket/directory where the query results land.
         */
        String inputBucketIdentifier,

        /*
         * The associated EEL transformation's manifest.
         */
        WorkbookValidator.Manifest transformationManifest,

        /*
         * The output bucket/directory where the transformation's output is sent.
         */
        String outputBucketIdentifier
) { }