package edu.columbia.cs.ltrie.excel.curves;

import java.io.Serializable;

import edu.columbia.cs.utils.Pair;

public interface RankingMethodCurve extends Serializable{
	public Pair<double[],double[]> getCurveRetrieval();
	public Pair<double[],double[]> getCurveExtraction();
	public boolean allowsRankingMetrics();
	public double getAveragePrecision();
	public double getAveragePrecisionStdDev();
	public double getRPrecision();
	public double getRPrecisionStdDev();
	public double getAreaUnderROC();
	public double getAreaUnderROCStdDev();
}
