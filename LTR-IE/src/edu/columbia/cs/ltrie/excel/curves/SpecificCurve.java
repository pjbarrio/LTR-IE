package edu.columbia.cs.ltrie.excel.curves;

import edu.columbia.cs.utils.Pair;

public class SpecificCurve implements RankingMethodCurve {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5939514678101699096L;
	private double[] x;
	private double[] y;
	
	public SpecificCurve(double[] x, double[] y){
		this.x = x;
		this.y = y;
	}

	@Override
	public Pair<double[], double[]> getCurveExtraction() {
		return new Pair<double[], double[]>(x, y);
	}
	
	@Override
	public Pair<double[], double[]> getCurveRetrieval() {
		return new Pair<double[], double[]>(x, y);
	}
	
	@Override
	public boolean allowsRankingMetrics() {
		return false;
	}

	@Override
	public double getAveragePrecision() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getRPrecision() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getAveragePrecisionStdDev() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getRPrecisionStdDev() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public double getAreaUnderROC() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public double getAreaUnderROCStdDev() {
		throw new UnsupportedOperationException();
	}
}
