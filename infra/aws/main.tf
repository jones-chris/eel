data "aws_caller_identity" "current" {}

locals {
  product     = "eel"
  stack_name  = "vpc-base-vpc"
  aws_account = data.aws_caller_identity.current.account_id
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      product = local.product
      region  = var.aws_region
      stack   = local.stack_name
    }
  }
}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "6.6.0"

  name = "any-etl-base-vpc"
  cidr = "10.0.0.0/16"

  azs = [
    "${var.aws_region}a",
    "${var.aws_region}b",
    "${var.aws_region}c",
    "${var.aws_region}d",
    "${var.aws_region}e",
    "${var.aws_region}f"
  ]

  # Private subnets (where the DB Query Runner Lambda Function and VPC endpoints will live)
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]

  # Public subnets (needed if you want a NAT Gateway for internet access)
  # public_subnets = ["10.0.101.0/24", "10.0.102.0/24"]

  # DNS Hostnames are REQUIRED for Secrets Manager "private_dns_enabled" to work
  enable_dns_hostnames = true
  enable_dns_support   = true

  # Optional: Set to true if your Lambda needs to talk to the public internet
  # enable_nat_gateway = false
  # single_nat_gateway = true
}

# Security Groups
# 1. The Security Group for the DB Query Runner Lambda Function
resource "aws_security_group" "db_query_runner_sg" {
  name        = "db-query-runner-sg"
  description = "Security group for the AnyETL DB Query Runner Lambda function"
  vpc_id      = module.vpc.vpc_id

  # Standard outbound rule to allow Lambda to talk to anything (including the VPCEs)
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# 2. The Security Group for the Secrets Manager Interface Endpoint
resource "aws_security_group" "secrets_manager_vpce_sg" {
  name        = "secrets-manager-vpce-sg"
  description = "Allow inbound HTTPS from Lambda SG"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "Allow ONLY incoming traffic from the DB query runner Lambda Function's SG"
    from_port       = 443
    to_port         = 443
    protocol        = "tcp"
    security_groups = [aws_security_group.db_query_runner_sg.id]
  }

  egress {
    description = "Standard Allow all outbound rule"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Create S3 gateway, secrets manager interface, DDB gateway and vpc endpoints.
# Note that we only need to allow traffic within the deployment region, not across regions.
resource "aws_vpc_endpoint" "s3" {
  vpc_id            = module.vpc.vpc_id
  service_name      = "com.amazonaws.${var.aws_region}.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = module.vpc.private_route_table_ids
}

resource "aws_vpc_endpoint" "dynamodb" {
  vpc_id            = module.vpc.vpc_id
  service_name      = "com.amazonaws.${var.aws_region}.dynamodb"
  vpc_endpoint_type = "Gateway"
  ip_address_type   = "ipv4"
  route_table_ids   = module.vpc.private_route_table_ids
}

resource "aws_vpc_endpoint" "secrets_manager" {
  vpc_id              = module.vpc.vpc_id
  service_name        = "com.amazonaws.${var.aws_region}.secretsmanager"
  vpc_endpoint_type   = "Interface"
  ip_address_type     = "ipv4"
  private_dns_enabled = true
  subnet_ids          = module.vpc.private_subnets
  security_group_ids  = [aws_security_group.secrets_manager_vpce_sg.id]

  dns_options {
    dns_record_ip_type                             = "ipv4"
    private_dns_only_for_inbound_resolver_endpoint = false
  }
}

# API services
module "manifest_services" {
  source = "./modules/manifest-service"

  aws_account = local.aws_account
  aws_region  = var.aws_region
}

module "engine" {
  source = "./modules/engine"

  aws_region = var.aws_region
}
