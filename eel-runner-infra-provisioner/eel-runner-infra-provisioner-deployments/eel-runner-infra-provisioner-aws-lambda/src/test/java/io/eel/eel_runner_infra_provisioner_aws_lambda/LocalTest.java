package io.eel.eel_runner_infra_provisioner_aws_lambda;

import io.eel.common.dao.FlowDao;
import io.eel.common.model.Flow;
import io.eel.common.model.ScheduledBatchConfiguration;
import io.eel.common_aws.AwsDynamoDbFlowDaoImpl;
import io.eel.eel_runner_infra_provisioner_aws_lambda.orchestrator.AwsEelBatchProcessorStackOrchestratorImpl;
import io.eel.eel_runner_infra_provisioner_aws_lambda.orchestrator.EelBatchProcessorStackOrchestrator;
import io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.AwsLambdaEelBatchProcessorStack;
import io.eel.eel_runner_infra_provisioner_core.stacks.EelBatchProcessorStack;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LocalTest {

//    private static AwsLambdaEelBatchProcessorStack processorStack;
//
//    public static void main(String[] args) {
//        try (
//                final SchedulerClient schedulerClient = SchedulerClient.create();
//                final LambdaClient lambdaClient = LambdaClient.create();
//                final S3Client s3Client = S3Client.create();
//                final SqsClient sqsClient = SqsClient.create();
//                final IamClient iamClient = IamClient.builder().build();
//                final SfnClient sfnClient = SfnClient.create();
//        ) {
//            processorStack = new AwsLambdaEelBatchProcessorStack(
//                    schedulerClient,
//                    lambdaClient,
//                    s3Client,
//                    sqsClient,
//                    iamClient,
//                    sfnClient
//            );
//
//            // todo:  remember to replace "#".
//            final String flowId = "b6ec0a10-ef89-4c0f-9ce9-4e516b942a18";
////            final String flowId = "b6ec0a10-ef89-4c0f-9ce9-4e516b942a17";
////            final String flowId = "8817065c-0e13-43ca-978f-544e899365e1";
//            final int version = 0;
//            final String canonicalId = flowId + "v" + version;
//            final String cronExpression = "cron(0/15 * * * ? *)";
//
//            final FlowDao flowDao = new AwsDynamoDbFlowDaoImpl(DynamoDbClient.create());
//            Set<String> sheetNames = flowDao.getFlowByCanonicalId(Flow.Utils.getCanonicalId(UUID.fromString(flowId), version))
//                            .map(Flow::getScheduledBatchConfiguration)
//                            .map(ScheduledBatchConfiguration::sheetQueries)
//                            .map(Map::keySet)
//                            .orElseThrow(() -> new RuntimeException("Could not find flow with canonical id of " + Flow.Utils.getCanonicalId(UUID.fromString(flowId), version)));
//
//
//        processorStack.deploy(canonicalId, cronExpression, flowId, version, sheetNames);
//        } catch (Throwable t) {
//            t.printStackTrace();
//
//            throw t;
//        }
//    }

    public static void main(String[] args) {

        EelBatchProcessorStackOrchestrator stackOrchestrator = new AwsEelBatchProcessorStackOrchestratorImpl();

        final String flowId = "b6ec0a10-ef89-4c0f-9ce9-4e516b942a18";
        final int version = 0;
        final FlowDao flowDao = new AwsDynamoDbFlowDaoImpl(DynamoDbClient.create());
        Flow flow = flowDao.getFlowByCanonicalId(Flow.Utils.getCanonicalId(UUID.fromString(flowId), version))
                .orElseThrow();

        stackOrchestrator.deploy(flow);

    }

//    public static void main(String[] args) {
//
//        EelBatchProcessorStackOrchestrator stackOrchestrator = new AwsEelBatchProcessorStackOrchestratorImpl();
//
//        final String flowId = "b6ec0a10-ef89-4c0f-9ce9-4e516b942a18";
//        final int version = 0;
//        final FlowDao flowDao = new AwsDynamoDbFlowDaoImpl(DynamoDbClient.create());
//        Flow flow = flowDao.getFlowByCanonicalId(Flow.Utils.getCanonicalId(UUID.fromString(flowId), version))
//                .orElseThrow();
//
//        stackOrchestrator.delete(flow);
//
//    }

}
