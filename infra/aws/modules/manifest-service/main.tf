# DDB table
resource "aws_dynamodb_table" "eel_manifests" {
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "transformationId"
  name           = "eel-manifests"
  stream_enabled = false

  attribute {
    name = "transformationId"
    type = "S"
  }
}

# Manifest API Lambda Function and IAM roles/policies
resource "aws_iam_role" "manifest_api" {
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
  description = "The IAM role of the Manifest API Lambda Function"
  name_prefix = "manifest-api-role-"
  path        = "/service-role/"
}

resource "aws_lambda_function" "manifest_api" {
  architectures    = ["x86_64"]
  description      = "The EEL Manifest API service"
  filename         = "../../manifest-api/manifest-api-deployments/manifest-api-aws-lambda/target/manifest-generator-aws-lambda-1.0-SNAPSHOT.jar"
  source_code_hash = filebase64sha256("../../manifest-api/manifest-api-deployments/manifest-api-aws-lambda/target/manifest-generator-aws-lambda-1.0-SNAPSHOT.jar")
  function_name    = "manifest-api"
  handler          = "io.eel.manifest_api_aws_lambda.RestApiHandler"
  memory_size      = 512
  package_type     = "Zip"
  role             = aws_iam_role.manifest_api.arn
  runtime          = "java21"
  timeout          = 300
}

resource "aws_cloudwatch_log_group" "manifest_api" {
  name              = "/aws/lambda/${aws_lambda_function.manifest_api.function_name}"
  retention_in_days = 30
}

resource "aws_iam_role_policy" "manifest_api" {
  name = "s3-access-policy"
  role = aws_iam_role.manifest_api.name

  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Effect" : "Allow",
        "Action" : "logs:CreateLogGroup",
        "Resource" : "arn:aws:logs:${var.aws_account}:${var.aws_account}:*"
      },
      {
        "Effect" : "Allow",
        "Action" : [
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ],
        "Resource" : [
          "${aws_cloudwatch_log_group.manifest_api.arn}:*"
        ]
      },
      {
        "Effect" : "Allow",
        "Action" : [
          "dynamodb:*" # todo:  restrict this
        ],
        "Resource" : [
          aws_dynamodb_table.eel_manifests.arn
        ]
      }
    ]
  })
}

# Manifest Event Consumer and IAM role/policy.  Note that this Lambda uses the same code as the Manifest API Lambda Function
# above but with a different entry point.
resource "aws_iam_role" "manifest_event_consumer" {
  assume_role_policy = jsonencode({
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
    Version = "2012-10-17"
  })

  description = "The IAM role for the Manifest Event Consumer Lambda Function"
  name_prefix = "manifest-event-consumer-role-"
  path        = "/service-role/"
}

resource "aws_lambda_function" "manifest_event_consumer" {
  architectures    = ["x86_64"]
  description      = "The EEL Manifest Lambda Function that is triggered upon a xlsx workbook being uploaded to S3"
  filename         = "../../manifest-api/manifest-api-deployments/manifest-api-aws-lambda/target/manifest-generator-aws-lambda-1.0-SNAPSHOT.jar"
  source_code_hash = filebase64sha256("../../manifest-api/manifest-api-deployments/manifest-api-aws-lambda/target/manifest-generator-aws-lambda-1.0-SNAPSHOT.jar")
  function_name    = "manifest-event-consumer"
  handler          = "io.eel.manifest_api_aws_lambda.S3PutObjectHandler"
  memory_size      = 512
  package_type     = "Zip"
  role             = aws_iam_role.manifest_event_consumer.arn
  runtime          = "java21"
  timeout          = 180
}

resource "aws_cloudwatch_log_group" "manifest_event_consumer" {
  name              = "/aws/lambda/${aws_lambda_function.manifest_event_consumer.function_name}"
  retention_in_days = 30
}

resource "aws_iam_role_policy" "manifest_event_consumer" {
  name = "s3-access-policy"
  role = aws_iam_role.manifest_event_consumer.name

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
          "${aws_cloudwatch_log_group.manifest_event_consumer.arn}:*"
        ]
      },
      {
        "Effect" : "Allow",
        "Action" : "s3:*", # todo: restrict this.
        "Resource" : [
          aws_s3_bucket.eel_manifest_service_staging.arn,
          "${aws_s3_bucket.eel_manifest_service_staging.arn}/*"
        ]
      },
      {
        "Effect" : "Allow",
        "Action" : [
          "dynamodb:*" # todo: restrict this.
        ],
        "Resource" : [
          aws_dynamodb_table.eel_manifests.arn
        ]
      }
    ]
  })
}

/*
  S3 Bucket and event notification to trigger Manifest Event Consumer Lambda to extract manifest from workbook when a
  workbook is uploaded.
 */
resource "aws_s3_bucket" "eel_manifest_service_staging" {
  bucket_prefix = "eel-staging-bucket-"
  region        = var.aws_region
}

resource "aws_s3_bucket_notification" "manifest_event_consumer_trigger" {
  bucket = aws_s3_bucket.eel_manifest_service_staging.id
  lambda_function {
    events              = ["s3:ObjectCreated:Put"]
    lambda_function_arn = aws_lambda_function.manifest_event_consumer.arn
  }
}

resource "aws_s3_bucket_cors_configuration" "eel_manifest_cors" {
  bucket = aws_s3_bucket.eel_manifest_service_staging.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["PUT"]
    allowed_origins = ["http://localhost:63342"] # todo:  fix this.
    expose_headers  = []
    max_age_seconds = 3000 # This is how long the browser caches the preflight response
  }
}
