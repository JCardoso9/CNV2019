package pt.ulisboa.tecnico.cnv.aws.balancer;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.ec2.AmazonEC2;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import pt.ulisboa.tecnico.cnv.parser.QueryParser;
import pt.ulisboa.tecnico.cnv.parser.Request;
import pt.ulisboa.tecnico.cnv.storage.DynamoDBStorage;
import pt.ulisboa.tecnico.cnv.storage.RequestMapping;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

import java.util.concurrent.Executors;

public class LoadBalancer{
	private static final int balancerPort = 8001;
	private static LoadBalancer loadBalancer;
	private static DynamoDBMapper mapper;
	private static double maxMetric = 0;
	private static double largestMapArea = 0;
	private static double distanceLargestMap = 0;
	private static int n = 10;

	/*private LoadBalancer() throws Exception{
		final HttpServer server = HttpServer.create(new InetSocketAddress(balancerPort), 0);

		server.createContext("/climb", new MyHandler());

		server.setExecutor(Executors.newCachedThreadPool());
		DynamoDBStorage.init();
		mapper = DynamoDBStorage.mapper;
		server.start();

	}*/

	public static void main(String[] args) throws Exception {
		final HttpServer server = HttpServer.create(new InetSocketAddress(balancerPort), 0);

		server.createContext("/climb", new MyHandler());

		server.setExecutor(Executors.newCachedThreadPool());
		DynamoDBStorage.init();
		mapper = new DynamoDBMapper(DynamoDBStorage.dynamoDB);
		server.start();
		System.out.println(server.getAddress().toString());
	}


	private static String GetLimitPoint(double radius, int point, boolean min, int datasetSize){
		if (min){
			double minimumPoint = point-radius;
			if (minimumPoint < 0){
				minimumPoint = 0;
			}
			return Double.toString(minimumPoint);
		}
		else{
			double maximumPoint = point + radius;
			if (maximumPoint > datasetSize){
				maximumPoint = datasetSize;
			}
			return Double.toString(maximumPoint);
		}
	}

	private static int GetDatasetSize(String dataset){
		//Assuming map is a square, modify to allow rectangles aswell.
		String[] splitDataset = dataset.split("_");
		if (splitDataset.length >= 3){
			int size = Integer.parseInt(splitDataset[2].split("x")[0]);
			if (largestMapArea < (size*size)){
				largestMapArea = size*size;
				distanceLargestMap = Math.sqrt((Math.pow((0-size),2) + Math.pow((0-size),2)));
			}
			return size;
		}
		return 0;
	}

	static List<RequestMapping> QueryDB(Request request){
		try{
			int datasetSize = GetDatasetSize(request.getDataset());
			//10% difference, 20% seems alot.
			double intervalAllowed = datasetSize * 0.1;
			Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
	        eav.put(":dataset", new AttributeValue().withS(request.getDataset()));
	        eav.put(":strategy", new AttributeValue().withS(request.getStrategy()));
	        eav.put(":minEntryX0" , new AttributeValue().withN(GetLimitPoint(intervalAllowed, request.getX0(), true, datasetSize)));
	        eav.put(":maxEntryX0" , new AttributeValue().withN(GetLimitPoint(intervalAllowed, request.getX0(), false, datasetSize)));
	        eav.put(":minEntryY0" , new AttributeValue().withN(GetLimitPoint(intervalAllowed, request.getY0(), true, datasetSize)));
	        eav.put(":maxEntryY0" , new AttributeValue().withN(GetLimitPoint(intervalAllowed, request.getY0(), false, datasetSize)));
	        eav.put(":minOutX1" , new AttributeValue().withN(GetLimitPoint(intervalAllowed, request.getX1(), true, datasetSize)));
	        eav.put(":maxOutX1" , new AttributeValue().withN(GetLimitPoint(intervalAllowed, request.getX1(), false, datasetSize)));
	        eav.put(":minOutY1" , new AttributeValue().withN(GetLimitPoint(intervalAllowed, request.getY1(), true, datasetSize)));
			eav.put(":maxOutY1" , new AttributeValue().withN(GetLimitPoint(intervalAllowed, request.getY1(), false, datasetSize)));
			eav.put(":minXs" , new AttributeValue().withN(GetLimitPoint(intervalAllowed, request.Xs(), false, datasetSize)));
			eav.put(":maxXs" , new AttributeValue().withN(GetLimitPoint(intervalAllowed, request.Xs(), false, datasetSize)));
			eav.put(":minYs" , new AttributeValue().withN(GetLimitPoint(intervalAllowed, request.Ys(), false, datasetSize)));
			eav.put(":maxYs" , new AttributeValue().withN(GetLimitPoint(intervalAllowed, request.Ys(), false, datasetSize)));

	        DynamoDBQueryExpression<RequestMapping> queryExpression = new DynamoDBQueryExpression<RequestMapping>()
	        		.withKeyConditionExpression("Dataset = :dataset")
	        		.withFilterExpression("Strategy = :strategy"
	                        + " and X0 between :minEntryX0 and :maxEntryX0"
	                        + " and Y0 between :minEntryY0 and :maxEntryY0"
	                        + " and X1 between :minOutX1 and :maxOutX1"
	                        + " and Y1 between :minOutY1 and :maxOutY1"
	                        + " and Xs between :minXs and :maxXs"
	                        + " and Xs between :minYs and :maxYs")
	                .withExpressionAttributeValues(eav);
	        List<RequestMapping> mapping = mapper.query(RequestMapping.class, queryExpression);
	        return mapping;
         } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to AWS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with AWS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
		return null;
		
	}

	static double CalculateWorstCaseDistance(int x0, int x1, int y0, int y1, int xs, int ys){
		int farthestPointX = (Math.abs(xs-x0) > Math.abs(x1-xs)) ? x0 : x1;
		int farthestPointY = (Math.abs(ys-y0) > Math.abs(y1-ys)) ? y0 : y1;
		return Math.sqrt((Math.pow((farthestPointX-xs),2) + Math.pow((farthestPointY-ys),2)));
	}

	static Request EstimateRequestComplexity(Request request){
		//Check if in cache later.
		System.out.println("Estimating request complexity....");
		List<RequestMapping> mappingList = QueryDB(request);
		double metricsAvg = -1;
		int metricsNumber = 0;
		if (mappingList.size() > 0){
			System.out.println("Found stuff in database");
			for (RequestMapping mapping : mappingList){
				if (mapping.getX0() == request.getX0() && mapping.getX1() == request.getX1() && mapping.getY0() == request.getY0()
					&& mapping.getY1() == request.getY1() && mapping.getXs() == request.Xs()){
					if (maxMetric < mapping.getMetrics()){
						maxMetric = mapping.getMetrics();
					}
					request.setEstimatedCost((mapping.getMetrics()/maxMetric)*n);
					break;
				}
				if (maxMetric < mapping.getMetrics()){
					maxMetric = mapping.getMetrics();
				}
				metricsAvg += mapping.getMetrics();
				metricsNumber++;
				//Might need some additional checks here.
			}
			metricsAvg = metricsAvg/metricsNumber;
			request.setEstimatedCost((metricsAvg/maxMetric)*n);
		}
		else{
			System.out.println("Nothing in db ");
			//Estimate cost.
			double mapArea = (request.getX1()-request.getX0())*(request.getY1()-request.getY0());
			double distanceWorstCase = CalculateWorstCaseDistance(request.getX0(),request.getX1(),request.getY0(),request.getY1(),request.Xs(),request.Ys());
			//This is incorrect and can give values above n, need to rethink formula;
			request.setEstimatedCost(((distanceWorstCase*mapArea) / (largestMapArea* distanceLargestMap))*n);
			System.out.println("Cost : " + Double.toString(((distanceWorstCase*mapArea) / (largestMapArea* distanceLargestMap)*n)));
			
		}
		return request;
	}

	static void SelectBestInstance(Request request){
		//Check, given request complexity and instances workload which instance is better suited.
	}

	static class MyHandler implements HttpHandler {
		private static AmazonEC2 ec2;
		private static ResourceBundle properties;
    	private static String REGION = "region";

		public MyHandler(){
			/*AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(new ProfileCredentialsProvider().getCredentials());
			properties = ResourceBundle.getBundle("ec2", Locale.ENGLISH);

			try{
            checkResourceBundleKeys(properties);
        }catch(Exception e){
        	e.printStackTrace();
		}
		ec2 = AmazonEC2ClientBuilder.standard().withRegion(properties.getString(REGION)).withCredentials(awsCredentials).build();*/
		}

		@Override
		public void handle(final HttpExchange t) throws IOException {
			final String query = t.getRequestURI().getQuery();
			Request request = new QueryParser().parseAndGetRequest(query);
			request.setRequestId(UUID.randomUUID().toString());
			System.out.println("Load Balancer received request, query: " + query);
			Long threadID = new Long(Thread.currentThread().getId());
			EstimateRequestComplexity(request);
			//InstanceClass instance = SelectBestInstance(request);

		}

		private static void checkResourceBundleKeys(ResourceBundle props) throws Exception {
        // If the required properties exist and have a value
	        if (!(props.containsKey(REGION)
	            || !(!props.getString(REGION).isEmpty()))) {
	            throw new Exception("Cannot load properties from file or they have no value." +
	                    " Make sure all properties have been declared.");
	        }
		}
	}

}
