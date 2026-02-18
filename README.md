#### EEL

### VPC Configuration

When provisioning EEL base infrastructure in your AWS account and you expect to run data source queries inside your VPCs,
please ensure you do the following:

1. Add the necessary VPC, subnet IDs, and security groups to the EEL Query Runner configuration so that it can connect to your SQL data sources.
2. Add a DynamoDB gateway endpoint, S3 gateway endpoint, and Secrets Manager interface endpoint (with private DNS resolution) so that 
EEL Query Runner can call these services.

