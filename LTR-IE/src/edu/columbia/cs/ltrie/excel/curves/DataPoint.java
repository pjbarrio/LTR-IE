package edu.columbia.cs.ltrie.excel.curves;

public class DataPoint implements Comparable<DataPoint> {
	private double score;
	private boolean label;
	
	public DataPoint (double score, boolean label) {
		this.score = score;
		this.label = label;
	}
	
	@Override
	public String toString() {
		return score + " " + (label ? "1" : "0");
	}

	@Override
	public int compareTo(DataPoint o) {
		return (int) Math.signum(o.score - score);
	}

	public double getScore() {
		return score;
	}

	public int getLabel() {
		return label ? 1 : 0;
	}
}
