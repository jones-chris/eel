variable "aws_region" {
  type        = string
  default     = "us-east-1"
  description = "The AWS region to deploy to"
}

# variable "tags" {
#   type        = map(string)
#   default     = {}
#   description = "The AWS resource tags to apply to all resources"
# }