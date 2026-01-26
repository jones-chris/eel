package io.eel.flow_api_aws_lambda.stacks.util;

import java.time.Duration;

public class Utils {

    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
