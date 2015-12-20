package edu.columbia.cs.ltrie.excel.curves;

import edu.columbia.cs.utils.Pair;

public class BaselineCurve implements RankingMethodCurve {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5939514678101699096L;
	private int numDocuments;
	
	public BaselineCurve(int numDocuments){
		this.numDocuments=numDocuments;
	}

	@Override
	public Pair<double[], double[]> getCurveRetrieval() {
		double[] x = new double[numDocuments+1];
		
		for(int i=0; i<=numDocuments; i++){
			x[i] = ((double)i)/((double)numDocuments);
		}
		
		return new Pair<double[], double[]>(x, x);
	}

	@Override
	public Pair<double[], double[]> getCurveExtraction() {
		double[] x = new double[numDocuments+1];
		
		for(int i=0; i<=numDocuments; i++){
			x[i] = ((double)i)/((double)numDocuments);
		}
		
		return new Pair<double[], double[]>(x, x);
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
