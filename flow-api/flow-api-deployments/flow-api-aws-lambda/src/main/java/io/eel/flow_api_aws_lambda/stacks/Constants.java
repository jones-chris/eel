package io.eel.flow_api_aws_lambda.stacks;

import java.time.Duration;

public class Constants {

    public final static Duration TEN_SECONDS = Duration.ofSeconds(10);

    public final static int ENGINE_TIMEOUT_IN_SECONDS = 120; // 2 minutes

    public final static int RETENTION_IN_DAYS = 60;  // 60 days or ~2 months

}
