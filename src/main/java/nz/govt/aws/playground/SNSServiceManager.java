package nz.govt.aws.playground;

import java.util.List;
import java.util.logging.Logger;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.Topic;

public class SNSServiceManager {

    static final Logger log = Logger.getLogger(SNSServiceManager.class.getName());
    private AmazonSNS amazonSNS;
    
	public SNSServiceManager(String region) {
		AmazonSNSClientBuilder amazonSNSClientBuilder = AmazonSNSClientBuilder.standard();
	    amazonSNSClientBuilder.setRegion(region);
		this.amazonSNS = amazonSNSClientBuilder.defaultClient();
		
		AWSSimpleSystemsManagementClientBuilder awsSsmClientBuilder = AWSSimpleSystemsManagementClientBuilder.standard();
    	awsSsmClientBuilder.setRegion(region); 	
	}

    public void notify(String topicName, String expectedTopicARN, String msg) {
    	String topicARN = getTopicArn(expectedTopicARN);
    	if (topicARN.isEmpty()) {
    		log.info("Topic not found. Proceed to create new topic '" + topicName + "'");
    		createNewSNSTopic(topicName);
    		subscripeToSNSTopicSMS(expectedTopicARN, "64210770605");
    	}
    	publishToSNSTopic(expectedTopicARN, msg);
     }
    
    public List<Topic> listTopics() {
    	ListTopicsResult listTopicsResult = amazonSNS.listTopics();
    	List<Topic> topicList = listTopicsResult.getTopics();
    	return topicList;
    }

    public void createNewSNSTopic(String topicName) {
    	//create a new SNS topic
    	CreateTopicRequest createTopicRequest = new CreateTopicRequest(topicName);
    	CreateTopicResult createTopicResult = amazonSNS.createTopic(createTopicRequest);
    	String topicArn = createTopicResult.getTopicArn();
    	log.info("New topic ARN :" + topicArn);	
    	log.info("CreateTopicRequest: " + amazonSNS.getCachedResponseMetadata(createTopicRequest));
    }
    
    public void subscribeToSNSTopic(String expectedTopicARN, String notificationType, String notificationDestination) {
    	//subscribe to an SNS topic
    	SubscribeRequest subRequest = new SubscribeRequest(expectedTopicARN, notificationType, notificationDestination);
    	amazonSNS.subscribe(subRequest);
    	//get request id for SubscribeRequest from SNS metadata
    	log.info("SubscribeRequest - " + amazonSNS.getCachedResponseMetadata(subRequest));
    }
    
    public void publishToSNSTopic(String topicARN, String msg) {
    	//publish to an SNS topic
    	PublishRequest publishRequest = new PublishRequest(topicARN, msg);
    	PublishResult publishResult = amazonSNS.publish(publishRequest);
    	//print MessageId of message published to SNS topic
    	log.info("MessageId: " + publishResult.getMessageId());
	}
    
    public String getTopicArn(String topicARN) {
    	ListTopicsResult listTopicsResult = amazonSNS.listTopics();
    	List<Topic> topicList = listTopicsResult.getTopics();
    	String resultTopicArn = "";

    	for (Topic topic : topicList) {
    		if(topic.getTopicArn().equals(topicARN)) {
    			resultTopicArn = topicARN;
    			log.info("Existing topic with ARN: " + resultTopicArn);
    			break;
    		}
    	}
    	
    	return resultTopicArn;
    }
    
    public void subscripeToSNSTopicEmail(String topicName, String emailAddress) {
    	subscribeToSNSTopic(topicName, "email", emailAddress);
    	log.info("Subscribing " + emailAddress + " to topic '" + topicName + "'");  
    }
    
    public void subscripeToSNSTopicSMS(String topicName, String phoneNumberWithCountryCode) {
    	subscribeToSNSTopic(topicName, "sms", phoneNumberWithCountryCode);
    	log.info("Subscribing +" + phoneNumberWithCountryCode + " to topic '" + topicName + "'");  
    }
}
