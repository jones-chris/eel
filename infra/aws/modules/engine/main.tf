locals {
  # The keys in the following map will be the S3 keys.  The values are the paths to the relevant JARs.
  base_lambda_jars = {
    "engine-deployments-aws-lambda.jar"                = "../../engine/engine-deployments/engine-deployments-aws-lambda/target/engine-deployments-aws-lambda-1.0-SNAPSHOT.jar",
    "database-query-runner-deployments-aws-lambda.jar" = "../../database-query-runner/database-query-runner-deployments/database-query-runner-deployments-aws-lambda/target/database-query-runner-deployments-aws-lambda-1.0-SNAPSHOT.jar"
  }
}

resource "aws_s3_bucket" "eel_base_engine" {
  bucket_prefix = "eel-engine-base-"
  region        = var.aws_region
}

resource "aws_s3_object" "base_jars" {
  for_each = local.base_lambda_jars

  bucket = aws_s3_bucket.eel_base_engine.id
  key    = each.key
  source = each.value

  # This ensures the file re-uploads if the JAR content changes
  source_hash = filebase64sha256(each.value)
}
