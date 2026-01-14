package io.eel.eel_runner_infra_provisioner_aws_lambda.util;

import java.time.Duration;

public class Utils {

    public static final Duration TEN_SECONDS = Duration.ofSeconds(10);

    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
