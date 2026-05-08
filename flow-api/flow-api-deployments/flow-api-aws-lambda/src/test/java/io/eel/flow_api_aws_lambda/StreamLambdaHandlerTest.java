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


class StreamLambdaHandlerTest {

    private final static String eventJsonString = """
       {
                    "version": "2.0",
                    "routeKey": "$default",
                    "rawPath": "/flow/new",
                    "rawQueryString": "",
                    "headers": {
                        "sec-fetch-mode": "cors",
                        "referer": "http://localhost:63342/",
                        "content-length": "0",
                        "x-amzn-tls-version": "TLSv1.3",
                        "sec-fetch-site": "cross-site",
                        "x-forwarded-proto": "https",
                        "accept-language": "en-US,en;q=0.5",
                        "origin": "http://localhost:63342",
                        "x-forwarded-port": "443",
                        "x-forwarded-for": "2600:1700:4171:3a90:1652:72c0:17f6:3118",
                        "priority": "u=4",
                        "accept": "*/*",
                        "x-amzn-tls-cipher-suite": "TLS_AES_128_GCM_SHA256",
                        "x-amzn-trace-id": "Root=1-68d096fe-4228d9f86b021c43192c4f7b",
                        "host": "5kv4haftclb3uzzltu4fjumsim0svguy.lambda-url.us-east-1.on.aws",
                        "accept-encoding": "gzip, deflate, br, zstd",
                        "user-agent": "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:143.0) Gecko/20100101 Firefox/143.0",
                        "sec-fetch-dest": "empty"
                    },
                    "requestContext": {
                        "accountId": "anonymous",
                        "apiId": "5kv4haftclb3uzzltu4fjumsim0svguy",
                        "domainName": "5kv4haftclb3uzzltu4fjumsim0svguy.lambda-url.us-east-1.on.aws",
                        "domainPrefix": "5kv4haftclb3uzzltu4fjumsim0svguy",
                        "http": {
                            "method": "POST",
                            "path": "/flow/new",
                            "protocol": "HTTP/1.1",
                            "sourceIp": "2600:1700:4171:3a90:1652:72c0:17f6:3118",
                            "userAgent": "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:143.0) Gecko/20100101 Firefox/143.0"
                        },
                        "requestId": "faba8bce-3456-4ca7-89b8-b374c069a432",
                        "routeKey": "$default",
                        "stage": "$default",
                        "time": "22/Sep/2025:00:23:26 +0000",
                        "timeEpoch": 1758500606418
                    },
                    "isBase64Encoded": false
                }
       \s
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