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

	@DynamoDBAttribute(attributeName="Y0")
	public int getY0(){
		return this.y0;
	}

	@DynamoDBAttribute(attributeName="X1")
	public int getX1(){
		return this.x1;
	}

	@DynamoDBAttribute(attributeName="Y1")
	public int getY1(){
		return this.y1;
	}

	@DynamoDBAttribute(attributeName="Xs")
	public int getXs(){
		return this.xs;
	}

	@DynamoDBAttribute(attributeName="Ys")
	public int getYs(){
		return this.ys;
	}

	@DynamoDBAttribute(attributeName="Strategy")
	public String getStrategy(){
		return this.strategy;
	}

	@DynamoDBHashKey(attributeName="Dataset")
	public String getDataset(){
		return this.dataset;
	}

	@DynamoDBAttribute(attributeName="Metric")
	public double getMetrics(){
		return this.metricResult;
	}

	@DynamoDBRangeKey(attributeName="RequestId")
	public String getRequestId(){
		return this.id;
	}
}