//package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;
//
//import software.amazon.awssdk.core.SdkBytes;
//import software.amazon.awssdk.services.iam.IamClient;
//import software.amazon.awssdk.services.iam.model.AttachRolePolicyRequest;
//import software.amazon.awssdk.services.lambda.LambdaClient;
//import software.amazon.awssdk.services.lambda.model.CreateEventSourceMappingRequest;
//import software.amazon.awssdk.services.lambda.model.CreateEventSourceMappingResponse;
//import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
//import software.amazon.awssdk.services.lambda.model.CreateFunctionResponse;
//import software.amazon.awssdk.services.lambda.model.Runtime;
//import software.amazon.awssdk.services.sqs.SqsClient;
//import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
//import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
//import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
//import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
//
//import java.io.File;
//import java.nio.file.Files;
//import java.util.logging.Logger;
//
///**
// * Service to orchestrate the creation and integration of an SQS queue
// * and an AWS Lambda function using AWS SDK v2.
// */
//public class LambdaSqsIntegrationService {
//
//    private static final Logger log = Logger.getLogger(LambdaSqsIntegrationService.class.getName());
//
//    // The ARN of the AWS managed policy that grants Lambda read/delete access to SQS
//    private static final String SQS_EXECUTION_POLICY_ARN = "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole";
//
//    private final SqsClient sqsClient;
//    private final LambdaClient lambdaClient;
//    private final IamClient iamClient;
//
//    public LambdaSqsIntegrationService(SqsClient sqsClient, LambdaClient lambdaClient, IamClient iamClient) {
//        this.sqsClient = sqsClient;
//        this.lambdaClient = lambdaClient;
//        this.iamClient = iamClient;
//    }
//
//    /**
//     * Builds the SQS queue, Lambda function, ensures permissions, and creates the event source mapping.
//     *
//     * @param functionName The name for the Lambda function.
//     * @param queueName The name for the SQS queue.
//     * @param lambdaRoleArn The ARN of the Lambda's existing execution role. (e.g., "arn:aws:iam::123456789012:role/MyLambdaRole")
//     */
//    public void setupSqsToLambdaIntegration(String functionName, String queueName, String lambdaRoleArn) {
//        // --- 1. ENSURE LAMBDA ROLE HAS SQS EXECUTION PERMISSIONS (THE FIX!) ---
//        log.info("Attaching SQS execution policy to role: " + lambdaRoleArn);
//        try {
//            AttachRolePolicyRequest attachRequest = AttachRolePolicyRequest.builder()
//                    .roleName(extractRoleNameFromArn(lambdaRoleArn))
//                    .policyArn(SQS_EXECUTION_POLICY_ARN)
//                    .build();
//
//            iamClient.attachRolePolicy(attachRequest);
//            log.info("Successfully attached SQS execution policy.");
//        } catch (Exception e) {
//            log.severe("Failed to attach SQS policy to IAM role: " + e.getMessage());
//            // It's possible the policy is already attached, but we handle other errors.
//            throw new RuntimeException("IAM Role configuration failed.", e);
//        }
//
//        // --- 2. CREATE SQS QUEUE ---
//        String sqsQueueArn;
//        String sqsQueueUrl;
//        try {
//            log.info("Creating SQS queue: " + queueName);
//            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
//                    .queueName(queueName)
//                    .build();
//
//            CreateQueueResponse createQueueResponse = sqsClient.createQueue(createQueueRequest);
//
//            // Get the URL needed for the next step
//            GetQueueUrlRequest getUrlRequest = GetQueueUrlRequest.builder().queueName(queueName).build();
//            GetQueueUrlResponse getUrlResponse = sqsClient.getQueueUrl(getUrlRequest);
//            sqsQueueUrl = getUrlResponse.queueUrl();
//
//            // SQS ARN is needed for EventSourceMapping, but we need to retrieve it.
//            // For simplicity, we'll use the URL in the EventSourceMapping later, but in a real app,
//            // you might need the ARN for other policy modifications.
//            // For EventSourceMapping, the SourceArn field expects the ARN. We'll use a placeholder for now.
//            // In a real application, you'd use GetQueueAttributesRequest to fetch the 'QueueArn'.
//            sqsQueueArn = "arn:aws:sqs:us-east-1:123456789012:" + queueName; // Mocking ARN
//
//            log.info("SQS Queue created successfully. ARN: " + sqsQueueArn);
//        } catch (Exception e) {
//            log.severe("Failed to create SQS queue: " + e.getMessage());
//            throw new RuntimeException("SQS Queue creation failed.", e);
//        }
//
//        // --- 3. CREATE LAMBDA FUNCTION ---
//        String lambdaFunctionArn;
//        try {
//            // Note: For a runnable example, you need a zipped JAR file.
//            // In a real app, 'eelJar' would be the File object from your original code.
//            // Using a simple mock zip file for demonstration (replace with actual file logic).
//            File mockLambdaCode = createMockZipFile();
//
//            log.info("Creating Lambda function: " + functionName);
//            CreateFunctionRequest createFunctionRequest = CreateFunctionRequest.builder()
//                    .functionName(functionName)
//                    .role(lambdaRoleArn)
//                    .runtime(Runtime.JAVA21)
//                    .handler("io.eel.engine_deployments_aws_lambda.S3PutObjectHandler") // Your existing handler
//                    .code(b -> b.zipFile(SdkBytes.fromFile(mockLambdaCode)))
//                    .memorySize(512)
//                    .timeout(300)
//                    .build();
//
//            CreateFunctionResponse createFunctionResponse = lambdaClient.createFunction(createFunctionRequest);
//            lambdaFunctionArn = createFunctionResponse.functionArn();
//            log.info("Lambda Function created successfully. ARN: " + lambdaFunctionArn);
//
//        } catch (Exception e) {
//            log.severe("Failed to create Lambda function: " + e.getMessage());
//            throw new RuntimeException("Lambda Function creation failed.", e);
//        }
//
//        // --- 4. CREATE EVENT SOURCE MAPPING (THE CONNECTION) ---
//        try {
//            log.info("Creating Event Source Mapping between SQS and Lambda...");
//            CreateEventSourceMappingRequest mappingRequest = CreateEventSourceMappingRequest.builder()
//                    .eventSourceArn(sqsQueueArn) // The ARN of the SQS queue
//                    .functionName(lambdaFunctionArn) // The ARN/Name of the Lambda function
//                    .batchSize(1) // Number of messages to process in a single batch
//                    .enabled(true)
//                    .build();
//
//            CreateEventSourceMappingResponse mappingResponse = lambdaClient.createEventSourceMapping(mappingRequest);
//            log.info("Event Source Mapping created successfully. UUID: " + mappingResponse.uuid());
//
//        } catch (Exception e) {
//            log.severe("Failed to create Event Source Mapping. The Lambda role may still lack permissions: " + e.getMessage());
//            throw new RuntimeException("Event Source Mapping failed.", e);
//        }
//    }
//
//    /**
//     * Helper to extract the Role Name from an ARN for the AttachRolePolicyRequest.
//     * @param arn The full role ARN.
//     * @return The role name.
//     */
//    private String extractRoleNameFromArn(String arn) {
//        // e.g., arn:aws:iam::123456789012:role/MyLambdaRole -> MyLambdaRole
//        return arn.substring(arn.lastIndexOf('/') + 1);
//    }
//
//    /**
//     * Mock method to simulate having a zipped Lambda function file.
//     * In a real app, replace this with your actual 'eelJar' File object.
//     */
//    private File createMockZipFile() {
//        try {
//            // Creates a dummy file. In a real scenario, this would be your
//            // compiled and zipped JAR file from the 'EelPackager.build' step.
//            File tempFile = File.createTempFile("mock-lambda-code", ".zip");
//            Files.write(tempFile.toPath(), "mock-zip-content".getBytes());
//            return tempFile;
//        } catch (Exception e) {
//            throw new RuntimeException("Could not create mock zip file.", e);
//        }
//    }
//
//    // In a production setup, you would initialize the clients like this:
//    /*
//    public static void main(String[] args) {
//        try (SqsClient sqsClient = SqsClient.builder().build();
//             LambdaClient lambdaClient = LambdaClient.builder().build();
//             IamClient iamClient = IamClient.builder().build()) {
//
//            LambdaSqsIntegrationService service = new LambdaSqsIntegrationService(sqsClient, lambdaClient, iamClient);
//
//            // Replace these with your actual names and ARN
//            String functionName = "MyEelFlowLambda";
//            String queueName = "MyEelFlowQueue";
//            String lambdaRoleArn = "arn:aws:iam::123456789012:role/MyExistingLambdaExecutionRole";
//
//            service.setupSqsToLambdaIntegration(functionName, queueName, lambdaRoleArn);
//            System.out.println("Integration setup complete.");
//        }
//    }
//    */
//}