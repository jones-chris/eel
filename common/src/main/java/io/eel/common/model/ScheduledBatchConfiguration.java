package io.eel.common.model;

import java.util.Map;

public record ScheduledBatchConfiguration(

        /*
         * The CRON expression that the flow runs on.
         */
        String cronExpression,

        /*
         * The queries that are executed for each of the manifest's input sheets.
         */
        Map<String, Query> sheetQueries
) { }