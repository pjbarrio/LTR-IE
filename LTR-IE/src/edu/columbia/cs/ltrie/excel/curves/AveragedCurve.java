package edu.columbia.cs.ltrie.excel.curves;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.columbia.cs.utils.Pair;

public class AveragedCurve implements RankingMethodCurve {
	
	private RankingMethodCurve[] curves;
	
	public AveragedCurve(RankingMethodCurve ... curves){
		this.curves=curves;
	}
	
	private boolean checkConsistency(Pair<double[], double[]>[] curves){
		int size=-1;
		for(int i=0; i<curves.length; i++){
			int curveSize = curves[i].first().length;
			if(size==-1){
				size=curveSize;
			}
			
			if(curveSize!=size){
				return false;
			}
		}
		return true;
	}

	@Override
	public Pair<double[], double[]> getCurveRetrieval() {
		Pair<double[], double[]>[] theCurves = new Pair[curves.length];
		for(int i=0; i<curves.length; i++){
			theCurves[i]=curves[i].getCurveRetrieval();
		}
		if(!checkConsistency(theCurves)){
			System.err.println("The curves are not consistent");
			System.err.println(Thread.currentThread().getStackTrace());
			System.exit(1);
		}
		
		double[] x = theCurves[0].first();
		double[] y = new double[x.length];
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
		if(!checkConsistency(theCurves)){
			System.err.println("The curves are not consistent");
			System.err.println(Thread.currentThread().getStackTrace());
			System.exit(1);
		}
		
		double[] x = theCurves[0].first();
		double[] y = new double[x.length];
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
