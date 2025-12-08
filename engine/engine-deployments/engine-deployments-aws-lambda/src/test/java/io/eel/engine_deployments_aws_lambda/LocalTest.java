package io.eel.engine_deployments_aws_lambda;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

public class LocalTest {

    public static void main(String[] args) {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        final String event = """
            {
                          "eventVersion": "2.0",
                          "eventSource": "aws:s3",
                          "awsRegion": "us-east-1",
                          "eventTime": "1970-01-01T00:00:00.000Z",
                          "eventName": "ObjectCreated:Put",
                          "userIdentity": {
                            "principalId": "EXAMPLE"
                          },
                          "requestParameters": {
                            "sourceIPAddress": "127.0.0.1"
                          },
                          "responseElements": {
                            "x-amz-request-id": "EXAMPLE123456789",
                            "x-amz-id-2": "EXAMPLE123/5678abcdefghijklambdaisawesome/mnopqrstuvwxyzABCDEFGH"
                          },
                          "s3": {
                            "s3SchemaVersion": "1.0",
                            "configurationId": "testConfigRule",
                            "bucket": {
                              "name": "eel-input-8817065c-0e13-43ca-978f-544e899365e1",
                              "ownerIdentity": {
                                "principalId": "EXAMPLE"
                              },
                              "arn": "arn:aws:s3:::example-bucket"
                            },
                            "object": {
                              "key": "eel_data.zip",
                              "size": 1024,
                              "eTag": "0123456789abcdef0123456789abcdef",
                              "sequencer": "0A1B2C3D4E5F678901"
                            }
                          }
                        }
        """;
        S3Event s3Event = gson.fromJson(event, S3Event.class);

        S3PutObjectHandler s3PutObjectHandler = new S3PutObjectHandler();
        s3PutObjectHandler.handleRequest(s3Event, new Context() {
            @Override
            public String getAwsRequestId() {
                return "";
            }

            @Override
            public String getLogGroupName() {
                return "";
            }

            @Override
            public String getLogStreamName() {
                return "";
            }

            @Override
            public String getFunctionName() {
                return "";
            }

            @Override
            public String getFunctionVersion() {
                return "";
            }

            @Override
            public String getInvokedFunctionArn() {
                return "";
            }

            @Override
            public CognitoIdentity getIdentity() {
                return null;
            }

            @Override
            public ClientContext getClientContext() {
                return null;
            }

            @Override
            public int getRemainingTimeInMillis() {
                return 0;
            }

            @Override
            public int getMemoryLimitInMB() {
                return 0;
            }

            @Override
            public LambdaLogger getLogger() {
                return null;
            }
        });
    }

}
