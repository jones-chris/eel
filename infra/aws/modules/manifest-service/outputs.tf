output "manifest_event_consumer_log_group_arn" {
  value = aws_cloudwatch_log_group.manifest_event_consumer.arn
}

output "manifest_api_log_group_arn" {
  value = aws_cloudwatch_log_group.manifest_api.arn
}

output "s3_staging_bucket_name" {
  value = aws_s3_bucket.eel_manifest_service_staging.id
}