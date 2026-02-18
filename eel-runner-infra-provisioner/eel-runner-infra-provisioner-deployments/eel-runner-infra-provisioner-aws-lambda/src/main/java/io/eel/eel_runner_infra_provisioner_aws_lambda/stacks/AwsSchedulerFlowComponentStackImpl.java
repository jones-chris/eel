package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import io.eel.common.model.Flow;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.*;

import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

import static io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.Constants.TEN_SECONDS;
import static io.eel.eel_runner_infra_provisioner_aws_lambda.util.Utils.sleep;
import static io.eel.eel_runner_infra_provisioner_core.stacks.model.ResourceType.*;

public class AwsSchedulerFlowComponentStackImpl extends FlowComponentStack {

    private static final Logger log = Logger.getLogger(AwsSchedulerFlowComponentStackImpl.class.getName());

    private static final String TRUST_POLICY = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {
                        "Service": "scheduler.amazonaws.com"
                      },
                      "Action": "sts:AssumeRole"
                    }
                  ]
                }
                """;

    private final IamClient iamClient;

    private final SchedulerClient schedulerClient;

    public AwsSchedulerFlowComponentStackImpl(
            IamClient iamClient,
            SchedulerClient schedulerClient,
            List<FlowComponentStack> dependentStacks
    ) {
        super(dependentStacks);

        this.iamClient = iamClient;
        this.schedulerClient = schedulerClient;

        this.addExpectedProvisionedResources(AWS_SCHEDULER_IAM_ROLE_NAME, AWS_SCHEDULER_NAME);

        super.addRollbackAction(RollbackActions.deleteRole(AWS_SCHEDULER_IAM_ROLE_NAME, iamClient))
//                .addRollbackAction(RollbackActions.deleteRolePolicy(AWS_SCHEDULER_IAM_ROLE_POLICY_NAME, iamClient))
                .addRollbackAction(RollbackActions.deleteScheduler(schedulerClient));
    }

    @Override
    public boolean deploy(Flow flow) {
        try {
            // Create the Scheduler role and policy so that Scheduler can assume the role and invoke the SFN.
            final Role role = this.iamClient.createRole(
                    CreateRoleRequest.builder()
                            .roleName("eel-scheduler-" + flow.getId())
                            .assumeRolePolicyDocument(TRUST_POLICY)
                            .build()
            ).role();

            this.provisionedResources.put(AWS_SCHEDULER_IAM_ROLE_NAME, role.roleName());

            // Sleep 10 seconds while the IAM role propagates in AWS.
            sleep(TEN_SECONDS);

            this.iamClient.putRolePolicy(
                     PutRolePolicyRequest.builder()
                             .roleName(role.roleName())
                             .policyName(role.roleName())
                             .policyDocument(this.buildPermissionsPolicy())
                             .build()
            );
//            this.provisionedResources.put(AWS_SCHEDULER_IAM_ROLE_POLICY_NAME, role.roleName());

            // Wait another 10 seconds while this policy change propagates throughout AWS.
            sleep(TEN_SECONDS);

            // todo:  consider putting unique UUID in input for the trace id?
            // Create the Scheduler instance.
//            final String input = gson.toJson(
//                    Map.of("canonicalId", canonicalId)
//            );

            String schedulerName = flow.getId().toString();
            String stepFunctionArn = this.getDependentResource(AWS_STEP_FUNCTION_ARN).orElseThrow();
            CreateScheduleRequest request = CreateScheduleRequest.builder()
                    .name(schedulerName)
                    .scheduleExpression(flow.getScheduledBatchConfiguration().cronExpression())
                    .target(
                            Target.builder()
                                    .arn(stepFunctionArn)
                                    .roleArn(role.arn())
                                    .build()
                    ).flexibleTimeWindow(FlexibleTimeWindow.builder().mode(FlexibleTimeWindowMode.OFF).build())
                    .build();

            CreateScheduleResponse response = this.schedulerClient.createSchedule(request);

            this.provisionedResources.put(AWS_SCHEDULER_NAME, schedulerName);

            log.info("Successfully created schedule " + flow.getId() + ", The ARN is " + response.scheduleArn());

            return true;
        } catch (Throwable t) {
            log.severe(t.getMessage());
            log.severe("Initiating rollback");

            this.rollback(flow);

            return false;
        }
    }

    @Override
    public boolean delete(String flowId, int version) {
        return false;  // todo:  implement this.
    }

    private String buildPermissionsPolicy() {
        String stepFunctionArn = this.getDependentResource(AWS_STEP_FUNCTION_ARN).orElseThrow();
        return """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Action": "states:StartExecution",
                            "Resource": "%s"
                        }
                    ]
                }
                """.formatted(stepFunctionArn);
    }

}
