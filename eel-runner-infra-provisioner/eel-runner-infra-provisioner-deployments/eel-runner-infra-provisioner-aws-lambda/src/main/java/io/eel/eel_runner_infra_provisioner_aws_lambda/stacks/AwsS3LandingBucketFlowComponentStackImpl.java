package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import io.eel.common.model.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import static io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.FlowComponentStack.ResourceType.AWS_S3_BUCKET_NAME;

public class AwsS3LandingBucketFlowComponentStackImpl extends FlowComponentStack {

    private static final Logger log = LoggerFactory.getLogger(AwsS3LandingBucketFlowComponentStackImpl.class);

    private final S3Client s3Client;

    public AwsS3LandingBucketFlowComponentStackImpl(S3Client s3Client) {
        super();

        this.s3Client = s3Client;

        super.addRollbackAction(RollbackActions.deleteS3Bucket(s3Client));
    }

    @Override
    public boolean deploy(Flow flow) {
        try {
            final String bucketName = "eel-input-" + flow.getId().toString();

            final CreateBucketRequest request = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            this.s3Client.createBucket(request);

            this.provisionedResources.put(AWS_S3_BUCKET_NAME, bucketName);

            return true;
        } catch (Throwable t) {
            log.error("Encountered error when trying to create bucket {}", flow.getId().toString());
            log.error("", t);

            this.rollback(flow);

            return false;
        }
    }

    @Override
    public boolean delete(String flowId, int version) {
        return false;
    }

}
