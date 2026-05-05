package io.eel.manifest_api_aws_lambda;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;

import java.util.List;

public class LocalTest {

    public static void main(String[] args) {
        S3Event s3Event = new S3Event(
                List.of(
                        new S3Event.S3EventNotificationRecord(
                                "us-east-1",
                                "aws:s3",
                                "ObjectCreated:Put",
                                "2024-06-01T12:00:00.000Z",
                                "2.1",
                                new S3Event.RequestParametersEntity("127.0.0.1"),
                                new S3Event.ResponseElementsEntity("x-amz-server-side-encryption", "AES256"),
                                new S3Event.S3Entity(
                                        "my-configuration-id",
                                        new S3Event.S3BucketEntity(
                                                "eel-staging-bucket-sljf34fa",
                                                new S3EventNotification.UserIdentityEntity("EXAMPLE"),
                                                "arn:aws:s3:::eel-staging-bucket-sljf34fa"
                                        ),
                                        new S3Event.S3ObjectEntity(
//                                                "artifacts/8f06bfbe-1e1f-45e1-aed2-e08365d6c8bc",
//                                                "8f06bfbe-1e1f-45e1-aed2-e08365d6c8bc",
//                                                "8f06bfbe-1e1f-45e1-aed2-e08365d6c8bc.xlsx",
                                                "artifacts/8f06bfbe-1e1f-45e1-aed2-e08365d6c8bd",
                                                539L,
                                                "ff07f137e58d7316be20739808c7dd75",
                                                "1",
                                                "123456789abcdef"
                                        ),
                                        null
                                ),
                                null
                        )
                )
        );

        S3PutObjectHandler handler = new S3PutObjectHandler();
        handler.handleRequest(s3Event, null);

    }
}
