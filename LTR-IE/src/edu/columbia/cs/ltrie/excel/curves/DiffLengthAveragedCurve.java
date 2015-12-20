package edu.columbia.cs.ltrie.excel.curves;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import edu.columbia.cs.utils.Pair;

public class DiffLengthAveragedCurve implements RankingMethodCurve {
	
	private RankingMethodCurve[] curves;
	private int minSize;
	
	public DiffLengthAveragedCurve(RankingMethodCurve ... curves){
		this.curves=curves;
		this.minSize = Integer.MAX_VALUE;
		for(int i=0; i<curves.length; i++){
			int curveSize = curves[i].getCurveExtraction().first().length;
			if (curveSize < minSize){
				minSize = curveSize;
			}
			
		}
		
	}
	

	@Override
	public Pair<double[], double[]> getCurveRetrieval() {
		Pair<double[], double[]>[] theCurves = new Pair[curves.length];
		for(int i=0; i<curves.length; i++){
			theCurves[i]=curves[i].getCurveRetrieval();
		}
		
		double[] x = Arrays.copyOf(theCurves[0].first(),minSize);
		double[] y = new double[minSize];
		for(int i=0; i<curves.length; i++){
			Pair<double[], double[]> currentCurve = theCurves[i];
			for(int j=0; j<x.length; j++){
				y[j]+=currentCurve.second()[j];
			}
		}
		for(int j=0; j<x.length; j++){
			y[j]/=curves.length;
		}
		
		
		return new Pair<double[], double[]>(x, y);
	}

	@Override
	public Pair<double[], double[]> getCurveExtraction() {
		Pair<double[], double[]>[] theCurves = new Pair[curves.length];
		for(int i=0; i<curves.length; i++){
			theCurves[i]=curves[i].getCurveExtraction();
		}
		
		
		double[] x = Arrays.copyOf(theCurves[0].first(),minSize);
		double[] y = new double[minSize];
		for(int i=0; i<curves.length; i++){
			Pair<double[], double[]> currentCurve = theCurves[i];
			for(int j=0; j<x.length; j++){
				y[j]+=currentCurve.second()[j];
			}
		}
		for(int j=0; j<x.length; j++){
			y[j]/=curves.length;
		}
		
		
		return new Pair<double[], double[]>(x, y);
	}

	@Override
	public boolean allowsRankingMetrics() {
		for(RankingMethodCurve curve : curves){
			if(!curve.allowsRankingMetrics()){
				return false;
			}
		}
		return true;
	}

	@Override
	public double getAveragePrecision() {
		double val = 0.0;
		for(RankingMethodCurve curve : curves){
			val+=curve.getAveragePrecision();
		}
		return val/curves.length;
	}

	@Override
	public double getRPrecision() {
		double val = 0.0;
		for(RankingMethodCurve curve : curves){
			val+=curve.getRPrecision();
		}
		return val/curves.length;
	}
	
	@Override
	public double getAreaUnderROC() {
		double val = 0.0;
		for(RankingMethodCurve curve : curves){
			val+=curve.getAreaUnderROC();
		}
		return val/curves.length;
	}

	@Override
	public double getAveragePrecisionStdDev() {
		double mean = 0.0;
		List<Double> values = new ArrayList<Double>();
		for(RankingMethodCurve curve : curves){
			double curveP =curve.getAveragePrecision();
			mean+=curveP;
			values.add(curveP);
		}
		mean/=curves.length;
		double stDev = 0.0;
		for(Double value : values){
			stDev += Math.pow(value-mean,2);
		}
		stDev/=curves.length;
		return Math.sqrt(stDev);
	}

	@Override
	public double getRPrecisionStdDev() {
		double mean = 0.0;
		List<Double> values = new ArrayList<Double>();
		for(RankingMethodCurve curve : curves){
			double curveP =curve.getRPrecision();
			mean+=curveP;
			values.add(curveP);
		}
		mean/=curves.length;
		double stDev = 0.0;
		for(Double value : values){
			stDev += Math.pow(value-mean,2);
		}
		stDev/=curves.length;
		return Math.sqrt(stDev);
	}

	@Override
	public double getAreaUnderROCStdDev() {
		double mean = 0.0;
		List<Double> values = new ArrayList<Double>();
		for(RankingMethodCurve curve : curves){
			double curveP =curve.getAreaUnderROC();
			mean+=curveP;
			values.add(curveP);
		}
		mean/=curves.length;
		double stDev = 0.0;
		for(Double value : values){
			stDev += Math.pow(value-mean,2);
		}
		stDev/=curves.length;
		return Math.sqrt(stDev);
	}
}
