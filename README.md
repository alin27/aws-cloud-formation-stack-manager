# aws-cloud-formation-stack-manager
Lambda functions using cloud formation templates stored in AWS S3 bucket to automatically create and delete OpenShift cloud formation stacks at an user-spceified time daily.

## Prerequisite
- OpenShift cloud formation templates 
- Red Hat subscription account
- Red Hat subscription to OpenShift container platform

## AWS Services Used
- Cloud formation
- Lambda
- EC2
- IAM
- SNS (SMS and email)
- System manager (stored parameters)

## Parameters Defined in System Manager
| Name  | Type | Description |
| ------------- | ------------- | ------------- |
| /OpenShift/CreateStacks/Dev/AvailabilityZones  | StringList  | List of availability zones. |
| /OpenShift/CreateStacks/Dev/ContainerAccessCIDR  | String  | Internal CIDR for container access. |
| /OpenShift/CreateStacks/Dev/KeyPairName  | String  | Name of the key pair to use for SSH. |
| /OpenShift/CreateStacks/Dev/MasterTemplateURL  | String  | URL of the cloud formation template for deploying OpenShift container platform into a new VPC. |
| /OpenShift/CreateStacks/Dev/OpenShiftAdminPassword  | SecureString  | Encrypted admin password. |
| /OpenShift/CreateStacks/Dev/RedhatSubscriptionPassword  | SecureString | Encrypted Red Hat subscription password. |
| /OpenShift/CreateStacks/Dev/RedhatSubscriptionPoolID  | String | Pool ID of OpenShift container platform subscription. |
| /OpenShift/CreateStacks/Dev/RedhatSubscriptionUserName  | String  | Red Hat subscription user name. |
| /OpenShift/CreateStacks/Dev/RemoteAccessCIDR  | String  | Internal CIDR for remote access. |
| /OpenShift/CreateStacks/Dev/StackName  | String  | Name of the stack. |
| /OpenShift/CreateStacks/Dev/TemplateURL  | String  | URL of the cloud formation template for deploying OpenShift container platform into an existing VPC. Private and public subnet IDs need to be specified. |
| /OpenShift/DeleteStacks/StackNamePrefix  | String | Used for stack deletion. Delete all stacks with name starts with this prefix. |
| /OpenShift/TopicARN  | String  | Integration with SNS. The topic ARN to publish notifications to.|

**(OPTIONAL)**
If the following parameters are found, the template will deploy OpenShift container platform into the existing VPC which those subnets belong to. Otherwise it will use the master template to create a new VPC.

| Name  | Type | Description |
| ------------- | ------------- | ------------- |
| /OpenShift/CreateStacks/Dev/PrivateSubnetIds  | StringList  | List of private subnet IDs. |
| /OpenShift/CreateStacks/Dev/PublicSubnetIds | StringList  | List of public subnet IDs. |

## Reference
[Red Hat OpenShfit Container Platform on AWS Cloud Quick Start Reference Deployment](https://aws.amazon.com/about-aws/whats-new/2017/09/red-hat-openshift-container-platform-on-the-aws-cloud-quick-start-reference-deployment/)

