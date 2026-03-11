package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import io.eel.common.model.Flow;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.Collections;
import java.util.logging.Logger;

import static io.eel.eel_runner_infra_provisioner_core.stacks.model.ResourceType.AWS_S3_BUCKET_ARN;
import static io.eel.eel_runner_infra_provisioner_core.stacks.model.ResourceType.AWS_S3_BUCKET_NAME;

public class AwsS3LandingBucketFlowComponentStackImpl extends FlowComponentStack {

    private static final Logger log = Logger.getLogger(AwsS3LandingBucketFlowComponentStackImpl.class.getName());

    private final S3Client s3Client;

    public AwsS3LandingBucketFlowComponentStackImpl(S3Client s3Client) {
        super();

        this.s3Client = s3Client;

        this.addExpectedProvisionedResources(AWS_S3_BUCKET_NAME, AWS_S3_BUCKET_ARN);

        super.addRollbackAction(RollbackActions.deleteS3Bucket(s3Client));
    }

    @Override
    public boolean deploy(Flow flow) {
        try {
            final String bucketName = "eel-input-" + flow.getId().toString();

            final CreateBucketRequest request = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            String bucketArn = this.s3Client.createBucket(request).bucketArn();

            // Set the provisioned resources before provisioning the bucket life cycle policy because we need to roll back
            // the bucket in the event that the bucket lifecycle policy encounters an error.
            this.provisionedResources.put(AWS_S3_BUCKET_NAME, bucketName);
            this.provisionedResources.put(AWS_S3_BUCKET_ARN, bucketArn);

            this.provisionBucketLifeCyclePolicy(bucketName);

            return true;
        } catch (Throwable t) {
            log.severe("Encountered error when trying to create bucket " + flow.getId().toString());
            log.severe(t.getMessage());

            this.rollback(flow);

            return false;
        }
    }

    private void provisionBucketLifeCyclePolicy(String bucketName) {
        // Define the expiration action (60 days ~ 2 months)
        LifecycleExpiration expiration = LifecycleExpiration.builder()
                .days(60)
                .build();

        // Create the lifecycle rule
        LifecycleRule rule = LifecycleRule.builder()
                .id("DeleteOldObjectsRule")
                .filter(LifecycleRuleFilter.builder().prefix("").build()) // Apply to all objects
                .status(ExpirationStatus.ENABLED)
                .expiration(expiration)
                .build();

        // Create the bucket lifecycle configuration
        BucketLifecycleConfiguration lifecycleConfiguration = BucketLifecycleConfiguration.builder()
                .rules(Collections.singletonList(rule))
                .build();

        // Apply the configuration to the bucket
        PutBucketLifecycleConfigurationRequest putRequest = PutBucketLifecycleConfigurationRequest.builder()
                .bucket(bucketName)
                .lifecycleConfiguration(lifecycleConfiguration)
                .build();

        this.s3Client.putBucketLifecycleConfiguration(putRequest);
    }

}
