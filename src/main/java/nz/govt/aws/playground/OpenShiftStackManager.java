package nz.govt.aws.playground;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException;

public class OpenShiftStackManager {

    static final Logger log = Logger.getLogger(OpenShiftStackManager.class.getName());
    private AmazonCloudFormation amazonCloudFormation;
    private AmazonEC2 amazonEC2;
    private SSMServiceManager ssmServiceManager;
    private SNSServiceManager snsServiceManager;
            
    //SSM parameters for delete stacks
    private static final String SSM_TOPIC_ARN = "/OpenShift/TopicARN";
    private static final String SSM_STACK_NAME_PREFIX = "/OpenShift/DeleteStacks/StackNamePrefix";
    
    private static final boolean WITH_DECRYPTION = true;
    private static final boolean NO_DECRYPTION= false;
    
    //SSM parameters for create stacks
    private static final String OPEN_SHIFT_ADMIN_PASSWORD_KEY = "OpenShiftAdminPassword";
    private static final String REDHAT_SUBSCRIPTION_USER_NAME_KEY = "RedhatSubscriptionUserName";
    private static final String REDHAT_SUBSCRIPTION_PASSWORD_KEY ="RedhatSubscriptionPassword";
    private static final String REDHAT_SUBSCRIPTION_POOL_ID_KEY = "RedhatSubscriptionPoolID";
    private static final String KEY_PAIR_NAME_KEY = "KeyPairName";
    private static final String REMOTE_ACCESS_CIDR_KEY = "RemoteAccessCIDR";
    private static final String CONTAINER_ACCESS_CIDR_KEY = "ContainerAccessCIDR";
    private static final String AVAILABILITY_ZONES_KEY = "AvailabilityZones";
    private static final String PRIVATE_SUBNETS_KEY = "PrivateSubnets";
    private static final String PUBLIC_SUBNETS_KEY = "PublicSubnets";
    private static final String VPCCIDR_KEY = "VPCCIDR";
    private static final List<String> PRIVATE_SUBNET_KEY_LIST = Arrays.asList("PrivateSubnet1CIDR","PrivateSubnet2CIDR", "PrivateSubnet3CIDR");
    private static final List<String> PUBLIC_SUBNET_KEY_LIST = Arrays.asList("PublicSubnet1CIDR","PublicSubnet2CIDR", "PublicSubnet3CIDR");
    
    private static final String SSM_STACK_NAME = "/OpenShift/CreateStacks/Dev/StackName";
    private static final String SSM_MASTER_TEMPLATE_URL = "/OpenShift/CreateStacks/Dev/MasterTemplateURL";   
    private static final String SSM_TEMPLATE_URL = "/OpenShift/CreateStacks/Dev/TemplateURL";
    private static final String SSM_OPEN_SHIFT_ADMIN_PASSWORD = "/OpenShift/CreateStacks/Dev/OpenShiftAdminPassword";
    private static final String SSM_REDHAT_SUBSCRIPTION_USER_NAME = "/OpenShift/CreateStacks/Dev/RedhatSubscriptionUserName";
    private static final String SSM_REDHAT_SUBSCRIPTION_PASSWORD = "/OpenShift/CreateStacks/Dev/RedhatSubscriptionPassword";
    private static final String SSM_REDHAT_SUBSCRIPTION_POOL_ID = "/OpenShift/CreateStacks/Dev/RedhatSubscriptionPoolID";
    private static final String SSM_KEY_PAIR_NAME = "/OpenShift/CreateStacks/Dev/KeyPairName"; 
    private static final String SSM_REMOTE_ACCESS_CIDR = "/OpenShift/CreateStacks/Dev/RemoteAccessCIDR";
    private static final String SSM_CONTAINER_ACCESS_CIDR = "/OpenShift/CreateStacks/Dev/ContainerAccessCIDR";
    private static final String SSM_AVAILABILITY_ZONES = "/OpenShift/CreateStacks/Dev/AvailabilityZones";
    private static final String SSM_OUTPUT_BUCKET_NAME = "/OpenShift/CreateStacks/Dev/OutputBucketName";
    private static final String SSM_PRIVATE_SUBNET_IDS = "/OpenShift/CreateStacks/Dev/PrivateSubnetIds";
    private static final String SSM_PUBLIC_SUBNET_IDS = "/OpenShift/CreateStacks/Dev/PublicSubnetIds";
    
    public OpenShiftStackManager(String region) {
    	AmazonCloudFormationClientBuilder amazonCloudFormationClientBuilder = AmazonCloudFormationClientBuilder.standard();
    	amazonCloudFormationClientBuilder.setRegion(region);
    	
    	AmazonEC2ClientBuilder amazonEC2ClientVuilder = AmazonEC2ClientBuilder.standard();
    	amazonEC2ClientVuilder.setRegion(region);
   
    	this.amazonCloudFormation = amazonCloudFormationClientBuilder.defaultClient();
    	this.ssmServiceManager = new SSMServiceManager(region);
    	this.snsServiceManager = new SNSServiceManager(region);
    	this.amazonEC2 = amazonEC2ClientVuilder.defaultClient();
    }
         
    public void deleteOpenShiftStacks() {
    	DescribeStacksResult describeStackResult = amazonCloudFormation.describeStacks();   	
		String msg = "";
    	
    	log.info("Describe stacks result: " + describeStackResult.toString());
    	
    	List<Stack> stackList = describeStackResult.getStacks();
    	log.info("Stacks: " + stackList.toString());
    	int stackDeletionCount = 0;
    	int totalNumberOfStacks = stackList.size();
    
		String stackPrefix = ssmServiceManager.getSSMParameter(SSM_STACK_NAME_PREFIX, NO_DECRYPTION);
		String topicARN = ssmServiceManager.getSSMParameter(SSM_TOPIC_ARN, NO_DECRYPTION);
		String topicName = getTopicNameFromARN(topicARN);
		
		log.info("Parametes: Stack name prefix - " + stackPrefix + ", topic ARN - " + topicARN + ", topic name - " + topicName);
		    	
    	for(Stack stack : stackList) {
    		String stackName = stack.getStackName().toString();
    		log.info("Current stack: " + stackName);
    		if (stackName.startsWith(stackPrefix)) {
    			log.info("Stack name starts with '" + stackPrefix + "', proceed to delete '" + stackName + "'");
    			DeleteStackRequest deleteStackRequest = new DeleteStackRequest();
    			deleteStackRequest.setStackName(stackName);
    			try {
	    			amazonCloudFormation.deleteStack(deleteStackRequest);
	    			stackDeletionCount ++;
    			} catch (Exception e) {
    				log.log(Level.SEVERE, "Stack deletion error: " + e.getMessage());
    				msg = new Date().toString() + ": Stack deletion error: " + e.getMessage();
    				snsServiceManager.notify(topicName,topicARN, msg);
    			}
    			
    		}
    	}  
    	msg = new Date().toString() + ": RedHat OpenShift delete stacks begins. Deleting " + stackDeletionCount + "/" + totalNumberOfStacks + 
    			" stacks with prefix '" + stackPrefix + "'";
    	snsServiceManager.notify(topicName, topicARN, msg);
    }
    
    public void createOpenShiftStacks() {
    	CreateStackRequest createStackRequest = generateCreateStackRequest();
    	String topicARN = ssmServiceManager.getSSMParameter(SSM_TOPIC_ARN, NO_DECRYPTION);
		String topicName = getTopicNameFromARN(topicARN);
		
		String msg = "";
    	
    	try {
    		amazonCloudFormation.createStack(createStackRequest);
    		msg = new Date().toString() + ": RedHat OpenShift create stacks begins. Creating stack '" + createStackRequest.getStackName() + "'";
    	}
    	catch (Exception e) {
    		log.log(Level.SEVERE, "Stack creation error: " + e.getMessage());
    		msg = new Date().toString() + ": Stack creation error: " + e.getMessage();
    	}
    	
    	snsServiceManager.notify(topicName, topicARN, msg);
    }
    
    public CreateStackRequest generateCreateStackRequest() {   	
    	//Create stack parameters
    	String stackName = ssmServiceManager.getSSMParameter(SSM_STACK_NAME, NO_DECRYPTION);
    	Map<String, String> subnetParameterMap = getSubnetParameters();
    	String templateURL = ssmServiceManager.getSSMParameter(SSM_TEMPLATE_URL, NO_DECRYPTION);
		
    	if (!allSubnetParametersAvailable(subnetParameterMap)) {
    		templateURL = ssmServiceManager.getSSMParameter(SSM_MASTER_TEMPLATE_URL, NO_DECRYPTION);
		}
		   	
    	CreateStackRequest createStackRequest = new CreateStackRequest();
    	createStackRequest.setStackName(stackName + "-" + generateTimeStamp());
    	createStackRequest.setTemplateURL(templateURL);
    	createStackRequest.setDisableRollback(true);

    	Collection<Parameter> parameters = setParameters(subnetParameterMap);
    	createStackRequest.setParameters(parameters);
       	createStackRequest.setCapabilities(Arrays.asList(Capability.CAPABILITY_IAM.toString()));
    	
    	return createStackRequest;
    }
    
	
	public Collection<Parameter> setParameters(Map<String, String> subnetParameterMap){
    	//Cloud formation template parameters
    	String openShiftAdminPassword = ssmServiceManager.getSSMParameter(SSM_OPEN_SHIFT_ADMIN_PASSWORD, WITH_DECRYPTION);
    	String redhatSubscriptionUserName = ssmServiceManager.getSSMParameter(SSM_REDHAT_SUBSCRIPTION_USER_NAME, NO_DECRYPTION);
    	String redhatSubscriptionPassword = ssmServiceManager.getSSMParameter(SSM_REDHAT_SUBSCRIPTION_PASSWORD, WITH_DECRYPTION);
    	String redhatSubscriptionPoolId = ssmServiceManager.getSSMParameter(SSM_REDHAT_SUBSCRIPTION_POOL_ID, NO_DECRYPTION);
    	String keyPairName = ssmServiceManager.getSSMParameter(SSM_KEY_PAIR_NAME, NO_DECRYPTION);
    	String remoteAccessCIDR = ssmServiceManager.getSSMParameter(SSM_REMOTE_ACCESS_CIDR, NO_DECRYPTION);
    	String containerAccessCIDR = ssmServiceManager.getSSMParameter(SSM_CONTAINER_ACCESS_CIDR, NO_DECRYPTION);
    	String availabilityZones = ssmServiceManager.getSSMParameter(SSM_AVAILABILITY_ZONES, NO_DECRYPTION);
    	
		Parameter openShiftAdminPasswordParam = setParameter(OPEN_SHIFT_ADMIN_PASSWORD_KEY, openShiftAdminPassword);
		Parameter redhatSubscriptionUserNameParam = setParameter(REDHAT_SUBSCRIPTION_USER_NAME_KEY, redhatSubscriptionUserName);
		Parameter redhatSubscriptionPasswordParam = setParameter(REDHAT_SUBSCRIPTION_PASSWORD_KEY, redhatSubscriptionPassword);
		Parameter redhatSubscriptionPoolIdParam = setParameter(REDHAT_SUBSCRIPTION_POOL_ID_KEY, redhatSubscriptionPoolId);
		Parameter keyPairNameParam = setParameter(KEY_PAIR_NAME_KEY, keyPairName);
		Parameter remoteAccessCIDRParam = setParameter(REMOTE_ACCESS_CIDR_KEY, remoteAccessCIDR);
		Parameter containerAccessCIDRParam = setParameter(CONTAINER_ACCESS_CIDR_KEY, containerAccessCIDR);
		Parameter availabilityZonesParam = setParameter(AVAILABILITY_ZONES_KEY, availabilityZones);
	
		List<Parameter> parameterList = generateParameterList(openShiftAdminPasswordParam, redhatSubscriptionUserNameParam, redhatSubscriptionPasswordParam,
				redhatSubscriptionPoolIdParam, keyPairNameParam, remoteAccessCIDRParam,containerAccessCIDRParam, availabilityZonesParam);
		
		if (allSubnetParametersAvailable(subnetParameterMap)) {		
			for (String privateSubnetKey : PRIVATE_SUBNET_KEY_LIST) {
				Parameter privateSubnetsParam = setParameter(privateSubnetKey,
						subnetParameterMap.get(privateSubnetKey).toString());
				parameterList.add(privateSubnetsParam);
			}
			
			for (String publicSubnetKey : PUBLIC_SUBNET_KEY_LIST) {
				Parameter publicSubnetsParam = setParameter(publicSubnetKey,
						subnetParameterMap.get(publicSubnetKey));
				parameterList.add(publicSubnetsParam);
			}
			
			//Get VPCCIDR using the first entry in private subnet list
			String subnetId = subnetParameterMap.get(PRIVATE_SUBNET_KEY_LIST.get(0));
			String vpccidr = getVPCCIDRFromSubnetId(subnetId);
			Parameter vpccidrParam = setParameter(VPCCIDR_KEY, vpccidr);
			parameterList.add(vpccidrParam);
		}
		
		log.info("Set " + parameterList.size() + " parameters (parameters: " + parameterList + ").");
    	return parameterList;
	}
	
	boolean allSubnetParametersAvailable(Map<String, String> subnetParameterMap) {
		boolean result = true;
		
		for (String subnetName : PRIVATE_SUBNET_KEY_LIST) {
			if(subnetParameterMap.get(subnetName) == null) {
				log.warning("Missing private subnet parameter '" + subnetName + "'");
				result = false;
			}
		}
		
		for (String subnetName : PUBLIC_SUBNET_KEY_LIST) {
			if(subnetParameterMap.get(subnetName) == null) {
				log.warning("Missing public subnet parameter '" + subnetName + "'");
				result = false;
			}
		}
		
		return result;
	}
	
	public Parameter setParameter(String parameterKey, String parameterValue) {
		Parameter parameter = new Parameter();
		parameter.setParameterKey(parameterKey);
		parameter.setParameterValue(parameterValue);
		return parameter;
		
	}
	public String getVPCCIDRFromSubnetId(String subnetId) {
		DescribeSubnetsRequest descriptbeSubnetRequest = new DescribeSubnetsRequest();
		descriptbeSubnetRequest.setSubnetIds(Arrays.asList(subnetId));
		DescribeSubnetsResult describeSubnetsResult = amazonEC2.describeSubnets(descriptbeSubnetRequest);
		String vpcId = describeSubnetsResult.getSubnets().get(0).getVpcId();
		log.info("Extracting VPC ID: "+ vpcId + "from subnet '" + subnetId + "'");
		return vpcId;
		
	}
	
	public List<Parameter> generateParameterList(Parameter... parameters){
		List<Parameter> parameterList = new ArrayList<Parameter>();
		for (Parameter parameter : parameters) {
			parameterList.add(parameter);
			log.info("Added '" + parameter.getParameterKey() + "' with value '" + parameter.getParameterValue() + "' to the list.");
		}
		return parameterList;
	}
	
	public Map<String, String> getSubnetParameters() {
		Map<String, String> resultMap = new HashMap<String,String>();
	
		//Get private subnets
		try {
			List<String> privateSubnetList = ssmServiceManager.getSSMParameterList(SSM_PRIVATE_SUBNET_IDS, NO_DECRYPTION);
			for (int index = 0; index < PRIVATE_SUBNET_KEY_LIST.size(); index ++) {
				log.info("Put in new element. Key: " + PRIVATE_SUBNET_KEY_LIST.get(index) + ". Value: " + privateSubnetList.get(index));
				resultMap.put(PRIVATE_SUBNET_KEY_LIST.get(index), privateSubnetList.get(index));
			}
			
		}catch (ParameterNotFoundException e) {
			log.info("No private subnets specified, using the master template which will create a new VPC.");
		}
		
		//Get public subnets
		try {
			List<String> publicSubnetList = ssmServiceManager.getSSMParameterList(SSM_PUBLIC_SUBNET_IDS, NO_DECRYPTION);
			for (int index = 0; index < PUBLIC_SUBNET_KEY_LIST.size(); index ++) {
				log.info("Put in new element. Key: " + PUBLIC_SUBNET_KEY_LIST.get(index) + ". Value: " + publicSubnetList.get(index));
				resultMap.put(PUBLIC_SUBNET_KEY_LIST.get(index), publicSubnetList.get(index));
			}
		}catch (ParameterNotFoundException e) {
			log.info("No public subnets specified, using the master template which will create a new VPC.");
		}
		
		return resultMap;		
	}
	
	public String getTopicNameFromARN(String topicARN) {
		String[] splittedString = topicARN.split(":");
		String topicName = splittedString[splittedString.length - 1];
		return topicName;
	}
	
	public String generateTimeStamp() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm"));
	}
}
