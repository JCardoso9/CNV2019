package pt.ulisboa.tecnico.cnv.aws.balancer;


import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.*;
import java.lang.Math.*;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import pt.ulisboa.tecnico.cnv.parser.*;
import pt.ulisboa.tecnico.cnv.storage.*;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

public class LoadBalancer{
	private static final int balancerPort = 8001;
	private static LoadBalancer loadBalancer;
	private static DynamoDBMapper mapper;
	private static double maxMetric = 0;
	private static double largestMapArea = 0;
	private static int n = 10;

	private LoadBalancer() throws Exception{
		final HttpServer server = HttpServer.create(new InetSocketAddress(balancerPort), 0);

		server.createContext("/climb", new MyHandler());

		server.setExecutor(Executors.newCachedThreadPool());
		DynamoDBStorage.init();
		mapper = DynamoDBStorage.mapper;
		server.start();

	}

	static LoadBalancer getInstance(){
		if (loadBalancer == null){
			try{
				loadBalancer = new LoadBalancer();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return loadBalancer;
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
			}
			return size;
		}
		return 0;
	}

	static List<RequestMapping> QueryDB(Request request){
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

		DynamoDBQueryExpression<RequestMapping> query = new DynamoDBQueryExpression<RequestMapping>()
                .withFilterExpression("Strategy = :strategy"
                		+ "Dataset = :dataset"
                        + " and X0 between :minEntryX0 and :maxEntryX0"
                        + " and Y0 between :minEntryY0 and :maxEntryY0"
                        + " and X1 between :minOutX1 and :maxOutX1"
                        + " and Y1 between :minOutY1 and :maxOutY1"
                        + " and XS between :minOutXs and :maxOutXs"
                        + " and YS between :minOutYs and :maxOutYs")
                .withExpressionAttributeValues(eav);

		return mapper.query(RequestMapping.class, query);
	}

	static double CalculateWorstCaseDistance(int x0, int x1, int y0, int y1, int xs, int ys){
		int farthestPointX = (Math.abs(xs-x0) > Math.abs(x1-xs)) ? x0 : x1;
		int farthestPointY = (Math.abs(ys-y0) > Math.abs(y1-ys)) ? y0 : y1;
		return Math.sqrt((Math.pow((farthestPointX-xs),2) + Math.pow((farthestPointY-ys),2)));
	}

	static Request EstimateRequestComplexity(Request request){
		//Check if in cache later.
		List<RequestMapping> mappingList = QueryDB(request);
		double metricsAvg = -1;
		int metricsNumber = 0;
		if (mappingList.size() > 0){
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
			//Estimate cost.
			double mapArea = (request.getX1()-request.getX0())*(request.getY1()-request.getY0());
			double distanceWorstCase = CalculateWorstCaseDistance(request.getX0(),request.getX1(),request.getY0(),request.getY1(),request.Xs(),request.Ys());
			//This is incorrect and can give values above n, need to rethink formula;
			request.setEstimatedCost = (distanceWorstCase / largestMapArea)*n;
			
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
			AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(new ProfileCredentialsProvider().getCredentials());
			properties = ResourceBundle.getBundle("ec2", Locale.ENGLISH);

			try{
            checkResourceBundleKeys(properties);
        }catch(Exception e){
        	e.printStackTrace();
		}
		ec2 = AmazonEC2ClientBuilder.standard().withRegion(properties.getString(REGION)).withCredentials(awsCredentials).build();
		}

		@Override
		public void handle(final HttpExchange t) throws IOException {
			final String query = t.getRequestURI().getQuery();
			Request request = new QueryParser().parseAndGetRequest(query);
			System.out.println("Load Balancer received reques, query: " + query);
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