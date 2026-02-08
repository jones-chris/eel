variable "aws_region" {
  type        = string
  default     = "us-east-1"
  description = "The AWS region to deploy to"
}

variable "aws_account" {
  type        = string
  description = "The AWS account to deploy to"
}