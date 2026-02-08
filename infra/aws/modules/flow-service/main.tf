# DDB tables
resource "aws_dynamodb_table" "eel_flows" {
  billing_mode                = "PAY_PER_REQUEST"
  deletion_protection_enabled = false
  hash_key                    = "id"
  name                        = "eel-flows"
  stream_enabled              = false

  attribute {
    name = "id"
    type = "S"
  }
}

resource "aws_dynamodb_table" "eel_flow_resources" {
  billing_mode                = "PAY_PER_REQUEST"
  deletion_protection_enabled = false
  hash_key                    = "id"
  name                        = "eel-flow-resources"
  stream_enabled              = false

  attribute {
    name = "id"
    type = "S"
  }
}

# Flow API Lambda Function and IAM role/policy.
resource "aws_iam_role" "flow_api_role" {
  assume_role_policy = jsonencode({
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
    Version = "2012-10-17"
  })
  description = "The IAM role for the EEL Flow API Lambda Function"
  name_prefix = "flow-api-role-"
  path        = "/service-role/"
}


resource "aws_lambda_function" "flow_api" {
  architectures    = ["x86_64"]
  description      = "The EEL Flow API Lambda Function"
  filename         = "../../flow-api/flow-api-deployments/flow-api-aws-lambda/target/flow-api-aws-lambda-1.0-SNAPSHOT.jar"
  source_code_hash = filebase64sha256("../../flow-api/flow-api-deployments/flow-api-aws-lambda/target/flow-api-aws-lambda-1.0-SNAPSHOT.jar")
  function_name    = "flow-api"
  handler          = "io.eel.flow_api_aws_lambda.StreamLambdaHandler"
  memory_size      = 512
  package_type     = "Zip"
  role             = aws_iam_role.flow_api_role.arn
  runtime          = "java21"
  timeout          = 30
  environment {
    variables = {
      S3_STAGING_BUCKET_NAME = var.s3_manifest_staging_bucket_name
    }
  }
}

resource "aws_cloudwatch_log_group" "flow_api" {
  name              = "/aws/lambda/${aws_lambda_function.flow_api.function_name}"
  retention_in_days = 30
}

/*
  The Flow API Lambda Function needs access to the S3 default AWS-owned KMS key for creating presigned URLs so users can
  upload workbooks.
 */
data "aws_kms_key" "s3_default" {
  key_id = "alias/aws/s3"
}

resource "aws_iam_role_policy" "manifest_api" {
  name = "s3-access-policy"
  role = aws_iam_role.flow_api_role.name

  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Effect" : "Allow",
        "Action" : "logs:CreateLogGroup",
        "Resource" : "arn:aws:logs:${var.aws_region}:${var.aws_account}:*"
      },
      {
        "Effect" : "Allow",
        "Action" : [
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ],
        "Resource" : [
          "${aws_cloudwatch_log_group.flow_api.arn}:*"
        ]
      },
      {
        "Effect" : "Allow",
        "Action" : [
          "dynamodb:*"
        ],
        "Resource" : aws_dynamodb_table.eel_flows.arn
      },
      {
        "Effect" : "Allow",
        "Action" : [
          "s3:PutObject"
        ],
        "Resource" : "arn:aws:s3:::${var.s3_manifest_staging_bucket_name}/*"
      },
      {
        "Effect" : "Allow",
        "Action" : "kms:*",
        "Resource" : data.aws_kms_key.s3_default.arn
      }
    ]
  })
}