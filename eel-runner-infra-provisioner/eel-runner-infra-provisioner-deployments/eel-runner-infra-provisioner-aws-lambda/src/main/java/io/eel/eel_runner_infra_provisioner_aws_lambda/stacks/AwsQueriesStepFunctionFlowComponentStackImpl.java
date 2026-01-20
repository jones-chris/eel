package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import com.amazonaws.services.stepfunctions.builder.StateMachine;
import com.amazonaws.services.stepfunctions.builder.states.Branch;
import com.amazonaws.services.stepfunctions.builder.states.ParallelState;
import com.amazonaws.services.stepfunctions.builder.states.State;
import com.amazonaws.services.stepfunctions.builder.states.TaskState;
import io.eel.common.model.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.CreateStateMachineRequest;
import software.amazon.awssdk.services.sfn.model.StateMachineType;

import java.util.List;
import java.util.Map;

import static com.amazonaws.services.stepfunctions.builder.StepFunctionBuilder.end;
import static com.amazonaws.services.stepfunctions.builder.StepFunctionBuilder.next;
import static io.eel.eel_runner_infra_provisioner_core.stacks.model.ResourceType.*;

public class AwsQueriesStepFunctionFlowComponentStackImpl extends FlowComponentStack {

    private static final Logger log = LoggerFactory.getLogger(AwsQueriesStepFunctionFlowComponentStackImpl.class);

    private static final String EEL_QUERY_RUNNER_ARN = System.getenv("EEL_QUERY_RUNNER_ARN");

    private static final String EEL_FLOWS_DYNAMODB_TABLE_ARN = System.getenv("EEL_FLOWS_TABLE_ARN");

    private static final String STEP_FUNCTION_TRUST_POLICY = """
                {
                  "Version": "2012-10-17",
                  "Statement": [{
                    "Effect": "Allow",
                    "Principal": { "Service": "states.amazonaws.com" },
                    "Action": "sts:AssumeRole"
                  }]
                }
                """;

    private SfnClient stepFunctionsClient;

    private IamClient iamClient;

    private AwsQueriesStepFunctionFlowComponentStackImpl() {}

    public AwsQueriesStepFunctionFlowComponentStackImpl(
        SfnClient stepFunctionsClient,
        IamClient iamClient,
        List<FlowComponentStack> dependentStacks
    ) {
        super(dependentStacks);

        this.stepFunctionsClient = stepFunctionsClient;
        this.iamClient = iamClient;

        this.addExpectedProvisionedResources(AWS_IAM_ROLE_ARN, AWS_STEP_FUNCTION_ARN);

        this.addRollbackAction(RollbackActions.deleteStepFunction(stepFunctionsClient))
                .addRollbackAction(RollbackActions.deleteRole(iamClient));
    }

    @Override
    public boolean deploy(Flow flow) {
        try {
            // Create the Parallel State with two identical branches
            ParallelState.Builder parallelProcessing = ParallelState.builder()
                    .comment("Run the sheet queries in parallel");

            for (String sheetName : flow.getScheduledBatchConfiguration().sheetQueries().keySet()) {
                State.Builder queryRunnerState = this.buildQueryRunnerState(flow, sheetName);
                parallelProcessing.branch(Branch.builder().startAt(sheetName).state(sheetName, queryRunnerState));
            }

            parallelProcessing.transition(next("NotifySQS"));

            // Create the state/stage that sends the queries' output to the input SQS queue. We use the parameters to
            // format the message using the output from the parallel branches that run the queries.
            String inputQueueUrl = this.getDependentResource(AWS_SQS_INPUT_QUEUE_URL).orElseThrow();
            State.Builder sendToSqs = TaskState.builder()
                    .resource("arn:aws:states:::sqs:sendMessage")
                    .parameters(
                            Map.of(
                                    "QueueUrl", inputQueueUrl,
                                    "MessageBody.$", "$[*].Payload"
                            )
                    ).transition(end());

            // Build the state machine/step function.
            StateMachine stateMachine = StateMachine.builder()
                    .startAt("ParallelProcessing")
                    .state("ParallelProcessing", parallelProcessing)
                    .state("NotifySQS", sendToSqs)
                    .build();;

            // Create the role
            Role role = this.iamClient.createRole(
                    CreateRoleRequest.builder()
                            .roleName("eel-sfn-" + flow.getId().toString())
                            .assumeRolePolicyDocument(STEP_FUNCTION_TRUST_POLICY)
                            .build()
            ).role();

            this.provisionedResources.put(AWS_IAM_ROLE_ARN, role.arn());

            // Attach the policy as an inline policy
            this.iamClient.putRolePolicy(PutRolePolicyRequest.builder()
                    .roleName(role.roleName())
                    .policyName(role.roleName())
                    .policyDocument(this.buildStepFunctionRolePolicy())
                    .build());

            CreateStateMachineRequest machineRequest = CreateStateMachineRequest.builder()
                    .definition(stateMachine.toPrettyJson())
                    .name(flow.getId().toString())
                    .roleArn(role.arn())
                    .type(StateMachineType.STANDARD)
                    .build();

            String stateMachineArn = this.stepFunctionsClient.createStateMachine(machineRequest)
                    .stateMachineArn();

            this.provisionedResources.put(AWS_STEP_FUNCTION_ARN, stateMachineArn);

            return true;
        } catch (Throwable t) {
            log.error("Encountered error when trying to create Lambda Function {}", flow.getId().toString());
            log.error("", t);

            this.rollback(flow);

            return false;
        }
    }

    @Override
    public boolean delete(String flowId, int version) {
        return false;
    }

    private State.Builder buildQueryRunnerState(Flow flow, String sheetName) {
        String landingBucketName = this.getDependentResource(AWS_S3_BUCKET_NAME).orElseThrow();
//        String landingBucketArn = "arn:aws:s3:::%s".formatted(landingBucketName);

        return TaskState.builder()
                .resource("arn:aws:states:::lambda:invoke")
                .parameters(
                        Map.of(
                                "FunctionName", EEL_QUERY_RUNNER_ARN,
                                "Payload", Map.of(
                                        "flowId", flow.getId().toString(),
                                        "version", flow.getVersion(),
                                        "inputSheet", sheetName,
                                        "destinationBucket", landingBucketName
                                )
                        )
                ).transition(end());
    }

    private String buildStepFunctionRolePolicy() {
        String landingBucketArn = this.getDependentResource(AWS_S3_BUCKET_ARN).orElseThrow();
        String inputQueueArn = this.getDependentResource(AWS_SQS_INPUT_QUEUE_ARN).orElseThrow();

        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Action": "s3:PutObject",
                      "Resource": "%s/*"
                    },
                    {
                      "Effect": "Allow",
                      "Action": "dynamodb:GetItem",
                      "Resource": "%s"
                    },
                    {
                      "Effect": "Allow",
                      "Action": "lambda:InvokeFunction",
                      "Resource": "%s"
                    },
                    {
                      "Effect": "Allow",
                      "Action": "sqs:SendMessage",
                      "Resource": "%s"
                    }
                  ]
                }
                """.formatted(
                    landingBucketArn,
                    EEL_FLOWS_DYNAMODB_TABLE_ARN,
                    EEL_QUERY_RUNNER_ARN,
                    inputQueueArn
                );
    }

}
