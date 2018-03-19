package nz.govt.aws.playground;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import com.amazonaws.regions.Regions;

public class OpenShiftStackManagerTest {
	
	private OpenShiftStackManager openShiftStackManager = new OpenShiftStackManager(Regions.US_EAST_1.toString());
	private static final List<String> PRIVATE_SUBNET_LIST_KEY = Arrays.asList("PrivateSubnet1CIDR","PrivateSubnet2CIDR", "PrivateSubnet3CIDR");
	private static final List<String> PUBLIC_SUBNET_LIST_KEY = Arrays.asList("PublicSubnet1CIDR","PublicSubnet2CIDR", "PublicSubnet3CIDR");

	@Test
	public void getTopicNameShouldExtractFromARN() {
		String topicARN = "arn:aws:sns:us-east-1:892674983559:OpenShiftDeleteStackTopic";
		String testTopicName = openShiftStackManager.getTopicNameFromARN(topicARN);
		assertEquals("OpenShiftDeleteStackTopic", testTopicName);
	}
	
	@Test
	public void shouldGenerateTimeStampCorrectly() {
		String testTimeStamp = openShiftStackManager.generateTimeStamp();
		System.out.println(testTimeStamp);
	}
	
	@Test
	public void shouldReturnFalseIfAnyParameterIsMissing() {
		Map<String, String> testMap = new HashMap<String, String>();
		testMap.put(PRIVATE_SUBNET_LIST_KEY.get(0), "test value");
		assertFalse(openShiftStackManager.allSubnetParametersAvailable(testMap));
		
		testMap.put(PRIVATE_SUBNET_LIST_KEY.get(1), "test value");
		assertFalse(openShiftStackManager.allSubnetParametersAvailable(testMap));
		
		testMap.put(PRIVATE_SUBNET_LIST_KEY.get(2), "test value");
		assertFalse(openShiftStackManager.allSubnetParametersAvailable(testMap));
		
		testMap.put(PUBLIC_SUBNET_LIST_KEY.get(0), "test value");
		assertFalse(openShiftStackManager.allSubnetParametersAvailable(testMap));
		
		testMap.put(PUBLIC_SUBNET_LIST_KEY.get(1), "test value");
		assertFalse(openShiftStackManager.allSubnetParametersAvailable(testMap));
		
		testMap.put(PUBLIC_SUBNET_LIST_KEY.get(2), "test value");
		assertTrue(openShiftStackManager.allSubnetParametersAvailable(testMap));		
	}
}
