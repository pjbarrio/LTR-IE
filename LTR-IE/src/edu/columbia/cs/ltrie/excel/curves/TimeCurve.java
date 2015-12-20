package edu.columbia.cs.ltrie.excel.curves;

import java.util.ArrayList;
import java.util.List;

import edu.columbia.cs.utils.Pair;

public class TimeCurve implements RankingMethodCurve {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5939514678101699096L;
	private RankingMethodCurve recallCurve;
	private List<Integer> timeCurve;
	private double extractionTimePerDocumentSecs = 0.001;
	
	public TimeCurve(RankingMethodCurve recallCurve, List<Integer> timeCurve, double averageTimeRelationshipInSeconds){
		this.recallCurve=recallCurve;
		this.timeCurve=timeCurve;
		extractionTimePerDocumentSecs = averageTimeRelationshipInSeconds;
	}
	
	private Pair<double[],double[]> getSpecificCurve(Pair<double[], double[]> recallCurve, List<Integer> timeCurve){
		double[] xAxis = recallCurve.second();
		double[] yAxis = new double[xAxis.length];
		yAxis[0]=timeCurve.get(0);
		for(int i=0; i<timeCurve.size(); i++){
			yAxis[i+1]=timeCurve.get(i);
		}
		
		List<Double> newXAxis = new ArrayList<Double>();
		List<Double> newYAxis = new ArrayList<Double>();
		double previousRecall = -1;
		for(int i=0; i<xAxis.length; i++){
			double newRecall = xAxis[i];
			if(newRecall!=previousRecall){
				newXAxis.add(newRecall);
				newYAxis.add(yAxis[i]+extractionTimePerDocumentSecs*i);
				previousRecall=newRecall;
			}
		}
		
		double[] finalXAxis = new double[newXAxis.size()];
		double[] finalYAxis = new double[newYAxis.size()];
		for(int i=0; i<newXAxis.size(); i++){
			finalXAxis[i] = newXAxis.get(i);
			finalYAxis[i] = newYAxis.get(i);
		}
						
		return new Pair<double[], double[]>(finalXAxis, finalYAxis);
	}

	@Override
	public Pair<double[], double[]> getCurveExtraction() {
		return getSpecificCurve(recallCurve.getCurveExtraction(),timeCurve);
	}
	
	@Override
	public Pair<double[], double[]> getCurveRetrieval() {
		return getSpecificCurve(recallCurve.getCurveRetrieval(),timeCurve);
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
