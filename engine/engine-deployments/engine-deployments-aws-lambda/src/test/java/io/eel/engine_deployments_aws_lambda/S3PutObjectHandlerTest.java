package io.eel.engine_deployments_aws_lambda;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.Gson;

import java.util.List;

class S3PutObjectHandlerTest {

    public static void main(String[] args) {
//        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
//        message.setBody("[{\"bucket\":\"eel-input-b6ec0a10-ef89-4c0f-9ce9-4e516b942a18\",\"key\":\"486037b3-792b-421f-afa2-c6e2a2af0d6e/input_customer.csv\",\"flowId\":\"b6ec0a10-ef89-4c0f-9ce9-4e516b942a18\",\"inputSheetName\":\"input_customer\"},{\"bucket\":\"eel-input-b6ec0a10-ef89-4c0f-9ce9-4e516b942a18\",\"key\":\"d484745d-3d28-4d62-9782-cf5ec3f1e9a8/input_customer_address.csv\",\"flowId\":\"b6ec0a10-ef89-4c0f-9ce9-4e516b942a18\",\"inputSheetName\":\"input_customer_address\"}]");

//        SQSEvent event = new SQSEvent();
//        event.setRecords(
//                List.of(message)
//        );

        Gson gson = new Gson();

        String eventString = """
            {Records: [{messageId: 53bf8040-db78-4bfc-aa49-fc4d18b622dd,receiptHandle: AQEB7g/TelGGPdpK+1T2gU/N/6m4aOHRo/GqWchH2fDoaxR+xLcMvoQYRzeh+/s6crriPAvGD9rXFiIJpJ3zFWgckymsmA0nU4/ke9owD5+Hr6x9MmKRmFfoT8KcEQaC54HFKlJH3x1+f8SWf7W/EOVwyXEgd5kn+ikCJu+BTe+yZoMmaxobiNDfBHkZuBPGaq+kTC+gCBV9N5j2H2D4Fl251M1lF9cSv/Ck7m29q2vPSLbpX1sIJAjsKS6EeNj3CJpv9u4ftCVO+z60uPQ9uuiBSUhpGYFI032rDRHNFBgC4SS7kEHG8bNMx5eTw2XQh6w460RejEnAUh9/uoosnKJKeDYvlat7GZ9EQxxB1RR0iGPgrZlHD5F4IaDKMRM7ScvwPVTsJiufzYVvIlFZZkFZjy1KMCgFwWw4c1spo+QczvKCU8wxEPl04MFKGMrQq5Gn,eventSourceARN: arn:aws:sqs:us-east-1:526661363425:eel-input-b6ec0a10-ef89-4c0f-9ce9-4e516b942a18,eventSource: aws:sqs,awsRegion: us-east-1,body: [{"ExecutedVersion":"$LATEST","Payload":{"bucket":"eel-input-b6ec0a10-ef89-4c0f-9ce9-4e516b942a18","key":"440da4e4-fb83-4755-b327-316563e33df3/input_customer.csv","flowId":"b6ec0a10-ef89-4c0f-9ce9-4e516b942a18","inputSheetName":"input_customer"},"SdkHttpMetadata":{"AllHttpHeaders":{"X-Amz-Executed-Version":["$LATEST"],"x-amzn-Remapped-Content-Length":["0"],"Connection":["keep-alive"],"x-amzn-RequestId":["9b111d0e-78f0-4104-ab87-f30dfceb5fc5"],"Content-Length":["205"],"Date":["Mon, 29 Dec 2025 01:19:15 GMT"],"X-Amzn-Trace-Id":["Root=1-6951d70a-3f43124672de60387a603d5d;Parent=4891daa21b62b07d;Sampled=0;Lineage=1:ddf3d3e0:0"],"Content-Type":["application/json"]},"HttpHeaders":{"Connection":"keep-alive","Content-Length":"205","Content-Type":"application/json","Date":"Mon, 29 Dec 2025 01:19:15 GMT","X-Amz-Executed-Version":"$LATEST","x-amzn-Remapped-Content-Length":"0","x-amzn-RequestId":"9b111d0e-78f0-4104-ab87-f30dfceb5fc5","X-Amzn-Trace-Id":"Root=1-6951d70a-3f43124672de60387a603d5d;Parent=4891daa21b62b07d;Sampled=0;Lineage=1:ddf3d3e0:0"},"HttpStatusCode":200},"SdkResponseMetadata":{"RequestId":"9b111d0e-78f0-4104-ab87-f30dfceb5fc5"},"StatusCode":200},{"ExecutedVersion":"$LATEST","Payload":{"bucket":"eel-input-b6ec0a10-ef89-4c0f-9ce9-4e516b942a18","key":"f226005e-0365-43b0-a4e2-eb053ae5b1f4/input_customer_address.csv","flowId":"b6ec0a10-ef89-4c0f-9ce9-4e516b942a18","inputSheetName":"input_customer_address"},"SdkHttpMetadata":{"AllHttpHeaders":{"X-Amz-Executed-Version":["$LATEST"],"x-amzn-Remapped-Content-Length":["0"],"Connection":["keep-alive"],"x-amzn-RequestId":["625fc99d-12ef-4c1a-be80-f0e7c69eda18"],"Content-Length":["221"],"Date":["Mon, 29 Dec 2025 01:19:14 GMT"],"X-Amzn-Trace-Id":["Root=1-6951d70a-401548b827d9494718f3bdf5;Parent=2ee66fce10f36523;Sampled=0;Lineage=1:ddf3d3e0:0"],"Content-Type":["application/json"]},"HttpHeaders":{"Connection":"keep-alive","Content-Length":"221","Content-Type":"application/json","Date":"Mon, 29 Dec 2025 01:19:14 GMT","X-Amz-Executed-Version":"$LATEST","x-amzn-Remapped-Content-Length":"0","x-amzn-RequestId":"625fc99d-12ef-4c1a-be80-f0e7c69eda18","X-Amzn-Trace-Id":"Root=1-6951d70a-401548b827d9494718f3bdf5;Parent=2ee66fce10f36523;Sampled=0;Lineage=1:ddf3d3e0:0"},"HttpStatusCode":200},"SdkResponseMetadata":{"RequestId":"625fc99d-12ef-4c1a-be80-f0e7c69eda18"},"StatusCode":200}],md5OfBody: 60bfa5319feebc2c83d7b0a023f15657,attributes: {ApproximateReceiveCount=1, SentTimestamp=1766971155163, SenderId=AROAXVH3WU3QVKA2UOJUI:KmBrskPEDbpVleFWOOnckynjiTLNiPmT, ApproximateFirstReceiveTimestamp=1766971155165},messageAttributes: {}}]}        
        """;

        SQSEvent event = gson.fromJson(eventString, SQSEvent.class);

        S3PutObjectHandler handler = new S3PutObjectHandler();

        handler.handleRequest(event, new Context() {
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
                return "b6ec0a10-ef89-4c0f-9ce9-4e516b942a18";
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