package pt.ulisboa.tecnico.cnv.storage;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import pt.ulisboa.tecnico.cnv.parser.*;

@DynamoDBTable(tableName="Requests")
public class RequestMapping{
	private int x0;
	private int x1;
	private int y0;
	private int y1;
	private int xs;
	private int ys;
	private String strategy;
	private String dataset;
	private double metricResult;
	private String id;

	public RequestMapping(){
	}

	public void setRequest(Request request){
		this.x0 = request.getX0();
		this.y0 = request.getY0();
		this.x1 = request.getX1();
		this.y1 = request.getY1();
		this.xs = request.Xs();
		this.ys = request.Ys();
		this.strategy = request.getStrategy();
		this.dataset = request.getDataset();
		this.id = request.getRequestId();
	}

	public void setMetric(long metric){
		this.metricResult = metric;
	}

	@DynamoDBAttribute(attributeName="X0")
	public int getX0(){
		return this.x0;
	}

	public void setX0(int x0){
		this.x0 = x0;
	}

	@DynamoDBAttribute(attributeName="Y0")
	public int getY0(){
		return this.y0;
	}

	public void setY0(int y0){
		this.y0 = y0;
	}

	@DynamoDBAttribute(attributeName="X1")
	public int getX1(){
		return this.x1;
	}

	public void setX1(int x1){
		this.x1 = x1;
	}

	@DynamoDBAttribute(attributeName="Y1")
	public int getY1(){
		return this.y1;
	}

	public void setY1(int y1){
		this.y1 = y1;
	}

	@DynamoDBAttribute(attributeName="Xs")
	public int getXs(){
		return this.xs;
	}

	public void setXs(int xs){
		this.xs = xs;
	}

	@DynamoDBAttribute(attributeName="Ys")
	public int getYs(){
		return this.ys;
	}

	public void setYs(int ys){
		this.ys = ys;
	}

	@DynamoDBAttribute(attributeName="Strategy")
	public String getStrategy(){
		return this.strategy;
	}

	public void setStrategy(String strategy){
		this.strategy =strategy;
	}

	@DynamoDBHashKey(attributeName="Dataset")
	public String getDataset(){
		return this.dataset;
	}

	public void setDataset(String dataset){
		this.dataset = dataset;
	}

	@DynamoDBAttribute(attributeName="Metric")
	public double getMetrics(){
		return this.metricResult;
	}

	public void setMetrics(double metrics){
		this.metricResult = metrics;
	}

	@DynamoDBRangeKey(attributeName="RequestId")
	public String getRequestId(){
		return this.id;
	}

	public void setRequestId(String requestId){
		this.id = requestId;
	}

	@Override
	public String toString(){
		return "X0: " + this.getX0() + "  X1 : " + this.getX1() + "  Y0: " + this.getY0() + " Y1: " + this.getY1() + " Xs: " + this.getXs() + " Ys : " + this.getYs() + " Dataset : " + this.getDataset() + " Strategy : " + this.getStrategy() + " Metrics : " + this.getMetrics() + " ID : " + this.getRequestId();
	}
}