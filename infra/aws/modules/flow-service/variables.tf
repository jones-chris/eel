variable "aws_region" {
  type        = string
  default     = "us-east-1"
  description = "The AWS region to deploy to"
}

variable "aws_account" {
  type        = string
  description = "The AWS account to deploy to"
}

variable "s3_manifest_staging_bucket_name" {
  type        = string
  description = "The name of the EEL manifest staging S3 bucket"
}