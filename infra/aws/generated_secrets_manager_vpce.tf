# # __generated__ by Terraform
# # Please review these resources and move them into your main configuration files.
#
# # __generated__ by Terraform
# resource "aws_vpc_endpoint" "secrets_manager2" {
#   auto_accept     = null
#   ip_address_type = "ipv4"
#   policy = jsonencode({
#     Statement = [{
#       Action    = "*"
#       Effect    = "Allow"
#       Principal = "*"
#       Resource  = "*"
#     }]
#   })
#   private_dns_enabled        = true
#   region                     = "us-east-1"
#   resource_configuration_arn = null
#   route_table_ids            = []
#   security_group_ids         = ["sg-05c7cd6f5432c1240"]
#   service_name               = "com.amazonaws.us-east-1.secretsmanager"
#   service_network_arn        = null
#   service_region             = "us-east-1"
#   subnet_ids                 = ["subnet-1c747d37", "subnet-1ea1c07b", "subnet-34d86a09", "subnet-b59753c3", "subnet-e8c7d8b1", "subnet-f146c5fd"]
#   tags                       = {}
#   tags_all                   = {}
#   vpc_endpoint_type          = "Interface"
#   vpc_id                     = "vpc-4422ac20"
#   dns_options {
#     dns_record_ip_type                             = "ipv4"
#     private_dns_only_for_inbound_resolver_endpoint = false
#     private_dns_specified_domains                  = []
#   }
#   subnet_configuration {
#     ipv4      = "172.31.12.207"
#     ipv6      = null
#     subnet_id = "subnet-b59753c3"
#   }
#   subnet_configuration {
#     ipv4      = "172.31.20.123"
#     ipv6      = null
#     subnet_id = "subnet-e8c7d8b1"
#   }
#   subnet_configuration {
#     ipv4      = "172.31.37.132"
#     ipv6      = null
#     subnet_id = "subnet-34d86a09"
#   }
#   subnet_configuration {
#     ipv4      = "172.31.60.131"
#     ipv6      = null
#     subnet_id = "subnet-1c747d37"
#   }
#   subnet_configuration {
#     ipv4      = "172.31.68.160"
#     ipv6      = null
#     subnet_id = "subnet-1ea1c07b"
#   }
#   subnet_configuration {
#     ipv4      = "172.31.82.53"
#     ipv6      = null
#     subnet_id = "subnet-f146c5fd"
#   }
# }
