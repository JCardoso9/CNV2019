package pt.ulisboa.tecnico.cnv.parser;

public class Request{
	private int x0;
	private int x1;
	private int y0;
	private int y1;
	private int xs;
	private int ys;
	private String strategy;
	private String dataset;
	private String rawQuery;
	private String requestId;
	private long estimatedCost;

	public Request(String requestId, int x0, int x1, int y0, int y1, int xs, int ys, String strategy, String dataset){
		this.requestId = requestId;
		this.x0 = x0;
		this.x1 = x1;
		this.y0 = y0;
		this.y1 = y1;
		this.xs = xs;
		this.ys = ys;
		this.strategy = strategy;
		this.dataset = dataset;
	}

	public void setRawQuery(String rawQuery){
		this.rawQuery = rawQuery;
	}

	public int getX0(){
		return this.x0;
	}
	public int getX1(){
		return this.x1;
	}
	public int getY0(){
		return this.y0;
	}
	public int getY1(){
		return this.y1;
	}
	public int Xs(){
		return this.xs;
	}
	public int Ys(){
		return this.ys;
	}
	public String getStrategy(){
		return this.strategy;
	}
	public String getDataset(){
		return this.dataset;
	}
	public String getRawQuery(){
		if (this.requestId != null && !this.requestId.isEmpty()){
			return this.rawQuery + "&id=" + this.requestId;
		}
		return this.rawQuery;
	}

	public String getRequestId(){
		return this.requestId;
	}

	public void setRequestId(String id){
		this.requestId = id;
	}

	public long getEstimatedCost(){
		return this.estimatedCost;
	}

	public void setEstimatedCost(long cost){
		this.estimatedCost = cost;
	}

	@Override
	public String toString(){
		return rawQuery;
	}
}