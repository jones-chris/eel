package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import io.eel.common.model.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.*;

import java.time.Instant;

import static io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.FlowComponentStack.ResourceType.AWS_SCHEDULER;
import static io.eel.eel_runner_infra_provisioner_aws_lambda.util.Utils.TEN_SECONDS;
import static io.eel.eel_runner_infra_provisioner_aws_lambda.util.Utils.sleep;

public class AwsSchedulerFlowComponentStackImpl extends FlowComponentStack {

    private static final Logger log = LoggerFactory.getLogger(AwsSchedulerFlowComponentStackImpl.class);

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

    private IamClient iamClient;

    private SchedulerClient schedulerClient;

    private final String stepFunctionArn;

    public AwsSchedulerFlowComponentStackImpl(
            IamClient iamClient,
            SchedulerClient schedulerClient,
            Flow flow,
            final String stepFunctionArn
    ) {
        super(flow);

        this.iamClient = iamClient;
        this.schedulerClient = schedulerClient;
        this.stepFunctionArn = stepFunctionArn;

        super.addRollbackAction(RollbackActions.deleteRole(iamClient))
                .addRollbackAction(
                        AWS_SCHEDULER,
                        (resourceId) -> {
                            this.schedulerClient.deleteSchedule(
                                    DeleteScheduleRequest.builder()
                                            .name(resourceId)
                                            .build()
                            );
                        }
                );
    }

    @Override
    public boolean deploy() {
        try {
            // Create the Scheduler role and policy so that Scheduler can assume the role and invoke the SFN.
            final Role role = this.iamClient.createRole(
                    CreateRoleRequest.builder()
                            .roleName("eel-scheduler-" + flow.getId())
                            .assumeRolePolicyDocument(TRUST_POLICY)
                            .build()
            ).role();

            this.provisionedResources.put(ResourceType.AWS_IAM_ROLE, role.roleName());

            // Sleep 10 seconds while the IAM role propagates in AWS.
            sleep(TEN_SECONDS);

             this.iamClient.putRolePolicy(
                     PutRolePolicyRequest.builder()
                             .roleName(role.roleName())
                             .policyName(role.roleName())
                             .policyDocument(this.buildPermissionsPolicy())
                             .build()
             );

            // Wait another 10 seconds while this policy change propagates throughout AWS.
            sleep(TEN_SECONDS);

            // Create the Scheduler instance.
//            final String input = gson.toJson(
//                    Map.of("canonicalId", canonicalId)
//            );

            final String schedulerName = flow.getId().toString();
            CreateScheduleRequest request = CreateScheduleRequest.builder()
                    .name(schedulerName)
                    .scheduleExpression(flow.getScheduledBatchConfiguration().cronExpression())
                    .target(
                            Target.builder()
                                    .arn(this.stepFunctionArn)
                                    .roleArn(role.arn())
                //                    .input(input)
                                    .build()
                    ).startDate(Instant.now())
                    .flexibleTimeWindow(FlexibleTimeWindow.builder().mode(FlexibleTimeWindowMode.OFF).build())
                    .build();

            CreateScheduleResponse response = this.schedulerClient.createSchedule(request);

            this.provisionedResources.put(AWS_SCHEDULER, schedulerName);

            log.info("Successfully created schedule {}, The ARN is {}", flow.getId(), response.scheduleArn());

            return true;
        } catch (Throwable t) {
            log.error("", t);
            log.error("Initiating rollback");

            this.rollback();

            return false;
        }
    }

    @Override
    public boolean delete(String flowId, int version) {
        return false;  // todo:  implement this.
    }

//    @Override
//    public boolean rollback() {
//        AtomicBoolean allResourcesWereDeleted = new AtomicBoolean(true);
//
//        try {
//            for (Map.Entry<ResourceType, String> entry : this.provisionedResources.reversed().entrySet()) {
//                final ResourceType resourceType = entry.getKey();
//                final String resourceId = entry.getValue();
//
//                switch (resourceType) {
//                    case AWS_SCHEDULER -> {
//                        boolean wasSuccessful = tryToDeleteResource(
//                                resourceId,
//                                () -> this.schedulerClient.deleteSchedule(
//                                        DeleteScheduleRequest.builder()
//                                                .name(resourceId)
//                                                .build())
//                        );
//
//                        if (!wasSuccessful) allResourcesWereDeleted.set(false);
//                    }
//                    case AWS_IAM_ROLE -> {
//                        boolean wasSuccessful = tryToDeleteResource(
//                                resourceId,
//                                () -> {
//                                    this.iamClient.deleteRole(
//                                            DeleteRoleRequest.builder()
//                                                    .roleName(resourceId)
//                                                    .build()
//                                    );
//
//                                    sleep(TEN_SECONDS);
//                                }
//                        );
//
//                        if (!wasSuccessful) allResourcesWereDeleted.set(false);
//                    }
//                    default -> throw new RuntimeException("Encountered unexpected resource type of " + resourceType + " for flow with id " + flow.getId());
//                }
//            }
//
//            return allResourcesWereDeleted.get();
//        } catch (Throwable t) {
//            log.error("", t);
//
//            return false;
//        }
//    }

    private String buildPermissionsPolicy() {
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
                """.formatted(this.stepFunctionArn);
    }

}
