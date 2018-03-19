package nz.govt.aws.playground;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;

public class SSMServiceManager {
	
	static final Logger log = Logger.getLogger(SSMServiceManager.class.getName());
	private AWSSimpleSystemsManagement amazonSSM;
	
	@SuppressWarnings("static-access")
	public SSMServiceManager(String region) {
		AWSSimpleSystemsManagementClientBuilder awsSSMClientBuilder = AWSSimpleSystemsManagementClientBuilder.standard();
    	awsSSMClientBuilder.setRegion(region);
		this.amazonSSM  = awsSSMClientBuilder.defaultClient();
	}
	
	public String getSSMParameter(String ssmName, boolean withDecryption) {
		GetParameterRequest getTopicArnParameterRequest = new GetParameterRequest().withName(ssmName).withWithDecryption(withDecryption);		
		GetParameterResult getParameterResult = amazonSSM.getParameter(getTopicArnParameterRequest);
		String value = getParameterResult.getParameter().getValue();
		
		if (withDecryption) {
			log.info("Parameter '" + ssmName + "' retrieved and decrypted from SSM.");
		}
		else {
			log.info("Parameter '" + ssmName + "' retrieved from SSM. Value: " + value);
		}
		
		return value;
	}
	
	public List<String> getSSMParameterList(String ssmName, boolean withDecryption) {
		String value = getSSMParameter(ssmName, withDecryption);
		List<String> valueList = Arrays.asList(value.split(","));
		
		return valueList;
	}

}
