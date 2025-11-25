package io.eel.eel_runner_infra_provisioner_aws_lambda;

import io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.AwsLambdaEelBatchProcessorStack;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.scheduler.SchedulerAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.UUID;

public class LocalTest {

    private static AwsLambdaEelBatchProcessorStack processorStack;

    public static void main(String[] args) {
        try (
                final SchedulerAsyncClient schedulerAsyncClient = SchedulerAsyncClient.create();
                final LambdaClient lambdaClient = LambdaClient.create();
                final S3Client s3Client = S3Client.create();
                final SqsClient sqsClient = SqsClient.create();
        ) {
            processorStack = new AwsLambdaEelBatchProcessorStack(
                    schedulerAsyncClient,
                    lambdaClient,
                    s3Client,
                    sqsClient
            );

            // todo:  remember to replace "#".
            final String canonicalId = "8817065c-0e13-43ca-978f-544e899365e1v0";
            final String cronExpression = "cron(0 12 * * ? *)";
            processorStack.deploy(canonicalId, cronExpression);
        } catch (Throwable t) {
            t.printStackTrace();

            throw t;
        }
    }

}
