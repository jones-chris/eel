package io.eel.flow_api_aws_lambda;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.eel.common.http.HttpResponse;

import java.lang.reflect.Type;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StreamLambdaHandlerTest {

    private final static String eventJsonString = """
        {
                  "body": {},
                  "resource": "/{proxy+}",
                  "path": "/flow/new",
                  "httpMethod": "POST",
                  "isBase64Encoded": true,
                  "queryStringParameters": {},
                  "multiValueQueryStringParameters": {
                    "foo": [
                      "bar"
                    ]
                  },
                  "pathParameters": {
                    "proxy": "/path/to/resource"
                  },
                  "stageVariables": {
                    "baz": "qux"
                  },
                  "headers": {
                    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
                    "Accept-Encoding": "gzip, deflate, sdch",
                    "Accept-Language": "en-US,en;q=0.8",
                    "Cache-Control": "max-age=0",
                    "CloudFront-Forwarded-Proto": "https",
                    "CloudFront-Is-Desktop-Viewer": "true",
                    "CloudFront-Is-Mobile-Viewer": "false",
                    "CloudFront-Is-SmartTV-Viewer": "false",
                    "CloudFront-Is-Tablet-Viewer": "false",
                    "CloudFront-Viewer-Country": "US",
                    "Host": "1234567890.execute-api.us-east-1.amazonaws.com",
                    "Upgrade-Insecure-Requests": "1",
                    "User-Agent": "Custom User Agent String",
                    "Via": "1.1 08f323deadbeefa7af34d5feb414ce27.cloudfront.net (CloudFront)",
                    "X-Amz-Cf-Id": "cDehVQoZnx43VYQb9j2-nvCh-9z396Uhbp027Y2JvkCPNLmGJHqlaA==",
                    "X-Forwarded-For": "127.0.0.1, 127.0.0.2",
                    "X-Forwarded-Port": "443",
                    "X-Forwarded-Proto": "https"
                  },
                  "multiValueHeaders": {
                    "Accept": [
                      "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
                    ],
                    "Accept-Encoding": [
                      "gzip, deflate, sdch"
                    ],
                    "Accept-Language": [
                      "en-US,en;q=0.8"
                    ],
                    "Cache-Control": [
                      "max-age=0"
                    ],
                    "CloudFront-Forwarded-Proto": [
                      "https"
                    ],
                    "CloudFront-Is-Desktop-Viewer": [
                      "true"
                    ],
                    "CloudFront-Is-Mobile-Viewer": [
                      "false"
                    ],
                    "CloudFront-Is-SmartTV-Viewer": [
                      "false"
                    ],
                    "CloudFront-Is-Tablet-Viewer": [
                      "false"
                    ],
                    "CloudFront-Viewer-Country": [
                      "US"
                    ],
                    "Host": [
                      "0123456789.execute-api.us-east-1.amazonaws.com"
                    ],
                    "Upgrade-Insecure-Requests": [
                      "1"
                    ],
                    "User-Agent": [
                      "Custom User Agent String"
                    ],
                    "Via": [
                      "1.1 08f323deadbeefa7af34d5feb414ce27.cloudfront.net (CloudFront)"
                    ],
                    "X-Amz-Cf-Id": [
                      "cDehVQoZnx43VYQb9j2-nvCh-9z396Uhbp027Y2JvkCPNLmGJHqlaA=="
                    ],
                    "X-Forwarded-For": [
                      "127.0.0.1, 127.0.0.2"
                    ],
                    "X-Forwarded-Port": [
                      "443"
                    ],
                    "X-Forwarded-Proto": [
                      "https"
                    ]
                  },
                  "requestContext": {
                    "accountId": "123456789012",
                    "resourceId": "123456",
                    "stage": "prod",
                    "requestId": "c6af9ac6-7b61-11e6-9a41-93e8deadbeef",
                    "requestTime": "09/Apr/2015:12:34:56 +0000",
                    "requestTimeEpoch": 1428582896000,
                    "identity": {
                      "cognitoIdentityPoolId": null,
                      "accountId": null,
                      "cognitoIdentityId": null,
                      "caller": null,
                      "accessKey": null,
                      "sourceIp": "127.0.0.1",
                      "cognitoAuthenticationType": null,
                      "cognitoAuthenticationProvider": null,
                      "userArn": null,
                      "userAgent": "Custom User Agent String",
                      "user": null
                    },
                    "path": "/prod/path/to/resource",
                    "resourcePath": "/{proxy+}",
                    "httpMethod": "POST",
                    "apiId": "1234567890",
                    "protocol": "HTTP/1.1"
                  }
                }       \s
   \s""";

    private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        Type empMapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> event = gson.fromJson(eventJsonString, empMapType);

        Context context = new Context() {
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
        };

        StreamLambdaHandler lambdaHandler = new StreamLambdaHandler();
        HttpResponse response = lambdaHandler.handleRequest(event, context);

        System.out.println(response.getBody());
    }

}