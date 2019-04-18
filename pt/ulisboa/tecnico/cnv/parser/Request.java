package pt.ulisboa.tecnico.cnv.parser;

public class Request{
	private int width;
	private int height;
	private int x0;
	private int x1;
	private int y0;
	private int y1;
	private int xs;
	private int ys;
	private String strategy;
	private String dataset;
	private String rawQuery;

	public Request(int w, int h, int x0, int x1, int y0, int y1, int xs, int ys, String strategy, String dataset){
		this.width = w;
		this.height = h;
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

	public int getWidth(){
		return width;
	}

	public int getHeight(){
		return this.height;
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
		return this.rawQuery;
	}

	@Override
	public String toString(){
		return rawQuery;
	}
}