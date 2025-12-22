package io.eel.eel_runner_infra_provisioner_aws_lambda;

import io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.AwsLambdaEelBatchProcessorStack;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.scheduler.SchedulerAsyncClient;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sqs.SqsClient;

public class LocalTest {

    private static AwsLambdaEelBatchProcessorStack processorStack;

    public static void main(String[] args) {
        try (
                final SchedulerAsyncClient schedulerAsyncClient = SchedulerAsyncClient.create();
                final LambdaClient lambdaClient = LambdaClient.create();
                final S3Client s3Client = S3Client.create();
                final SqsClient sqsClient = SqsClient.create();
                final IamClient iamClient = IamClient.builder().build();
                final SfnClient sfnClient = SfnClient.create();
                ) {
            processorStack = new AwsLambdaEelBatchProcessorStack(
                    schedulerAsyncClient,
                    lambdaClient,
                    s3Client,
                    sqsClient,
                    iamClient,
                    sfnClient
            );

            // todo:  remember to replace "#".
            final String flowId = "8817065c-0e13-43ca-978f-544e899365e1";
            final int version = 0;
            final String canonicalId = flowId + "v" + version;
            final String cronExpression = "cron(0 12 * * ? *)";
            processorStack.deploy(canonicalId, cronExpression, flowId);
        } catch (Throwable t) {
            t.printStackTrace();

            throw t;
        }
    }

}
