package pt.ulisboa.tecnico.cnv.storage;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import java.util.*;
import pt.ulisboa.tecnico.cnv.parser.*;

public class DynamoDBStorage{
	public static AmazonDynamoDB dynamoDB;
	public static DynamoDBMapper mapper;
	static Map<Long, Request> requestInformation = new HashMap<>();

	public static void init() throws Exception {
        String tableName = "Requests";
        /*
         * The ProfileCredentialsProvider will return your [default]
         * credential profile by reading from the credentials file located at
         * (~/.aws/credentials).
         */
        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
        try {
            credentialsProvider.getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (~/.aws/credentials), and is in valid format.",
                    e);
        }
	    dynamoDB = AmazonDynamoDBClientBuilder.standard()
	            .withCredentials(credentialsProvider)
	            .withRegion(Regions.US_EAST_1)
	            .build();
        mapper = new DynamoDBMapper(dynamoDB);
	    CreateTableRequest createTableRequest = mapper.generateCreateTableRequest(RequestMapping.class);
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(5L, 5L));
        TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);
        TableUtils.waitUntilActive(dynamoDB, tableName);
        DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
        TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
        System.out.println("Table Description: " + tableDescription);
    }	

    //Decide which metric to store
    public static void storeMetricsGathered(long threadID, long metric){
    	Request correspondingRequest = requestInformation.get(threadID);
    	RequestMapping mappedRequest = mapper.load(RequestMapping.class, correspondingRequest.getDataset(),correspondingRequest.getRequestId());
    	//Already exists in DB
    	if (mappedRequest == null){
    		mappedRequest = new RequestMapping();
    		mappedRequest.setRequest(correspondingRequest);
    	}
		mappedRequest.setMetric(metric);
		//Save updated isntance into database
        System.out.println(mappedRequest);
		mapper.save(mappedRequest);
    }	

    //Need to store the estimates for new requests aswell.

    public static void setNewRequest(long threadID, Request request){
    	requestInformation.put(threadID, request);
    }

    public static List<RequestMapping> getStoredMetrics(){
    	HashMap<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
		eav.put(":v0", new AttributeValue().withN("0"));
    	DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
    		.withFilterExpression("metricResult > :v0")
    		.withExpressionAttributeValues(eav);

		return mapper.scan(RequestMapping.class, scanExpression);
    }
}