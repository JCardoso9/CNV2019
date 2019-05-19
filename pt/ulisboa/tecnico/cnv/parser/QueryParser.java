package pt.ulisboa.tecnico.cnv.parser;
import java.util.HashMap;

public class QueryParser{
	private HashMap<String,String> argumentsMap = new HashMap<String,String>();
	private String rawQuery;
	private Request resultingRequest;

	public QueryParser(){

	}

	public Request parseAndGetRequest(String rawQuery){
		this.rawQuery = rawQuery;
		parseQuery();
		resultingRequest = createRequest();
		return resultingRequest;
	}

	private void parseQuery(){
		if (rawQuery != null && !rawQuery.isEmpty()){
			String[] splitString = rawQuery.split("&");
			for (String argument : splitString){
				String[] argumentValue = argument.split("=");
				if (argumentValue.length != 1 && !argumentValue[1].isEmpty()){
					argumentsMap.put(argumentValue[0], argumentValue[1]);
				}
				else{
					argumentsMap.put(argumentValue[0], "");
				}
			}
		}
	}

	private Request createRequest(){
		Request req = new Request(getRequestId(), parseInt(argumentsMap.get("x0"))
			,parseInt(argumentsMap.get("x1")),parseInt(argumentsMap.get("y0")),parseInt(argumentsMap.get("y1")),parseInt(argumentsMap.get("xS"))
			,parseInt(argumentsMap.get("yS")), argumentsMap.get("s"), argumentsMap.get("i"));
		if (argumentsMap.get("id") != null){
			req.setRequestId(argumentsMap.get("id"));
		}
		req.setRawQuery(rawQuery);
		return req;
	}

	public Request getRequest(){
		return resultingRequest;
	}

	private int parseInt(String s){
		return Integer.parseInt(s);
	}

	private String getRequestId(){
		if (argumentsMap.get("id") == null){
			return null;
		}
		return argumentsMap.get("id");
	}
}