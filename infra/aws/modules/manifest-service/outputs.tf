output "manifest_event_consumer_log_group_arn" {
  value = aws_cloudwatch_log_group.manifest_event_consumer.arn
}

output "manifest_api_log_group_arn" {
  value = aws_cloudwatch_log_group.manifest_api.arn
}