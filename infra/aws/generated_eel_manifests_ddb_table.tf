# # __generated__ by Terraform
# # Please review these resources and move them into your main configuration files.
#
# # __generated__ by Terraform
# resource "aws_dynamodb_table" "eel_manifests" {
#   billing_mode                = "PAY_PER_REQUEST"
#   deletion_protection_enabled = false
#   hash_key                    = "transformationId"
#   name                        = "eel-manifests"
#   range_key                   = null
#   read_capacity               = 0
#   region                      = "us-east-1"
#   restore_date_time           = null
#   restore_source_name         = null
#   restore_source_table_arn    = null
#   restore_to_latest_time      = null
#   stream_enabled              = false
#   table_class                 = "STANDARD"
#   tags                        = {}
#   tags_all                    = {}
#   write_capacity              = 0
#   attribute {
#     name = "transformationId"
#     type = "S"
#   }
#   point_in_time_recovery {
#     enabled                 = false
#     recovery_period_in_days = 0
#   }
#   ttl {
#     attribute_name = null
#     enabled        = false
#   }
# }
