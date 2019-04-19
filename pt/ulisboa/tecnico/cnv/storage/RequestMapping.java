package pt.ulisboa.tecnico.cnv.storage;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import pt.ulisboa.tecnico.cnv.parser.*;

@DynamoDBTable(tableName="Requests")
public class RequestMapping{
	private Request request;
	private long metricResult;

	public RequestMapping(){
	}

	public void setRequest(Request request){
		this.request = request;
	}

	public void setMetric(long metric){
		this.metricResult = metric;
	}

	@DynamoDBAttribute(attributeName="Request")
	public Request getRequest(){
		return this.request;
	}

	@DynamoDBAttribute(attributeName="Metric")
	public long getMetrics(){
		return this.metricResult;
	}

	@DynamoDBHashKey(attributeName="RequestId")
	public long getRequestId(){
		return request.getRequestId();
	}
}