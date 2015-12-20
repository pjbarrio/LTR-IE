package edu.columbia.cs.ltrie.excel.curves;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.columbia.cs.utils.Pair;

public class PerfectCurve implements RankingMethodCurve {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5619438572569752294L;
	private int numDocuments;
	private int numTuples;
	private Map<String,Integer> relevantDocuments;
	private List<Integer> sortedList;
	
	public PerfectCurve(int numDocuments, Map<String,Integer> relevantDocuments){
		this.numDocuments=numDocuments;
		this.relevantDocuments=relevantDocuments;
		numTuples=0;
		sortedList=new ArrayList<Integer>();
		for(Integer numT : relevantDocuments.values()){
			numTuples+=numT;
			sortedList.add(numT);
		}
		Collections.sort(sortedList);
	}

	@Override
	public Pair<double[], double[]> getCurveRetrieval() {
		double[] x = new double[numDocuments+1];
		double[] y = new double[numDocuments+1];
		
		int numRelevantDocuments = relevantDocuments.size();
		for(int i=0; i<=numDocuments; i++){
			x[i] = ((double)i)/((double)numDocuments);
			if(i<=numRelevantDocuments){
				y[i]=((double)i)/((double)numRelevantDocuments);
			}else{
				y[i]=1.0;
			}
		}
		
		return new Pair<double[], double[]>(x, y);
	}

	@Override
	public Pair<double[], double[]> getCurveExtraction() {
		double[] x = new double[numDocuments+1];
		double[] y = new double[numDocuments+1];
		
		int numRelevantDocuments = sortedList.size();
		int currentTuples=0;
		for(int i=0; i<=numDocuments; i++){
			x[i] = ((double)i)/((double)numDocuments);
			if(i==0){
				y[i]=0.0;
			}else if(i<=numRelevantDocuments){
				currentTuples+=sortedList.get(numRelevantDocuments-i);
				y[i]=((double)currentTuples)/((double)numTuples);
			}else{
				y[i]=1.0;
			}
		}
		
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
