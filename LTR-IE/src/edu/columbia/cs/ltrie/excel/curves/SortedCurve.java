package edu.columbia.cs.ltrie.excel.curves;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.columbia.cs.utils.Pair;

public class SortedCurve implements RankingMethodCurve {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8945704237353161950L;
	private int numDocuments;
	private int numTuples;
	private List<String> sortedDocuments;
	private Map<String,Integer> relevantDocuments;
	private String name;
	
	public SortedCurve(int numDocuments, List<String> sortedDocuments, Map<String,Integer> relevantDocuments){
		this.sortedDocuments=sortedDocuments;
		this.relevantDocuments=relevantDocuments;
		this.numDocuments=numDocuments;
		numTuples=0;
		for(Integer numT : relevantDocuments.values()){
			numTuples+=numT;
		}
	}
	
	protected void setRelevantDocuments(Map<String,Integer> relevantDocuments){
		this.relevantDocuments=relevantDocuments;
		numTuples=0;
		for(Integer numT : relevantDocuments.values()){
			numTuples+=numT;
		}
	}
	
	protected void setNumDocuments(int numDocuments){
		this.numDocuments=numDocuments;
	}
	
	protected void setSortedDocuments(List<String> sortedDocuments){
		this.sortedDocuments=sortedDocuments;
	}

	@Override
	public Pair<double[], double[]> getCurveRetrieval() {
		int numSortedDocuments = sortedDocuments.size();
		int numRelevantDocuments = relevantDocuments.size();
		double[] x = new double[numDocuments+1];
		double[] y = new double[numDocuments+1];
		
		int currentNumRelDocs=0;
		double accPrecision=0;
		double maximumRecall=0;
		for(int i=0; i<=numDocuments; i++){
			x[i] = ((double)i)/((double)numDocuments);
			double rel=0;
			if(i==0){
				y[i] = 0.0;
			}else if(i<=numSortedDocuments){
				String currentDocument = sortedDocuments.get(i-1);
				if(relevantDocuments.containsKey(currentDocument)){
					rel=1.0;
					currentNumRelDocs++;
				}
				y[i] = ((double)currentNumRelDocs)/((double)numRelevantDocuments);
				maximumRecall=Math.max(maximumRecall, y[i]);
			}else{
				y[i] = maximumRecall;
			}
			
			if(i!=0){
				double precisionAtI = (double)currentNumRelDocs/(double)i;
				accPrecision+=precisionAtI*rel;
			}
		}
		
		
		
		return new Pair<double[], double[]>(x, y);
	}

	public Pair<int[], int[]> getAbsoluteCurveRetrieval() {
		int numSortedDocuments = sortedDocuments.size();
		int[] x = new int[numDocuments+1];
		int[] y = new int[numDocuments+1];
		
		int currentNumRelDocs=0;
		for(int i=0; i<=numDocuments; i++){
			x[i] = i;
			if(i==0){
				y[i] = 0;
			}else if(i<=numSortedDocuments){
				String currentDocument = sortedDocuments.get(i-1);
				Integer val = relevantDocuments.get(currentDocument);
				if(val!=null && val != 0){
					currentNumRelDocs++;
				}
				
				y[i] = currentNumRelDocs;
			}else{
				y[i] = Integer.MIN_VALUE;
			}
		}
		
		return new Pair<int[], int[]>(x, y);
	}
	
	public Pair<int[], int[]> getAbsoluteCurveExtraction() {
		int numSortedDocuments = sortedDocuments.size();
		int[] x = new int[numDocuments+1];
		int[] y = new int[numDocuments+1];
		
		int currentNumTuples=0;
		for(int i=0; i<=numDocuments; i++){
			x[i] = i;
			if(i==0){
				y[i] = 0;
			}else if(i<=numSortedDocuments){
				String currentDocument = sortedDocuments.get(i-1);
				Integer val = relevantDocuments.get(currentDocument);
				if(val==null){
					val=0;
				}
				currentNumTuples+=val;
				y[i] = currentNumTuples;
			}else{
				y[i] = Integer.MIN_VALUE;
			}
		}
		
		return new Pair<int[], int[]>(x, y);
	}
	
	@Override
	public Pair<double[], double[]> getCurveExtraction() {
		int numSortedDocuments = sortedDocuments.size();
		double[] x = new double[numDocuments+1];
		double[] y = new double[numDocuments+1];
		
		int currentNumTuples=0;
		for(int i=0; i<=numDocuments; i++){
			x[i] = ((double)i)/((double)numDocuments);
			if(i==0){
				y[i] = 0.0;
			}else if(i<=numSortedDocuments){
				String currentDocument = sortedDocuments.get(i-1);
				Integer val = relevantDocuments.get(currentDocument);
				if(val==null){
					val=0;
				}
				currentNumTuples+=val;
				y[i] = ((double)currentNumTuples)/((double)numTuples);
			}else{
				y[i] = Double.MIN_VALUE;
			}
		}
		
		return new Pair<double[], double[]>(x, y);
	}
	
	public void setName(String name){
		this.name=name;
	}
	
	@Override
	public boolean allowsRankingMetrics() {
		return true;
	}

	@Override
	public double getAveragePrecision() {
		int numSortedDocuments = sortedDocuments.size();
		int numRelevantDocuments = relevantDocuments.size();
		int currentNumRelDocs=0;
		double accPrecision=0;
		for(int i=0; i<=numDocuments; i++){
			double rel=0;
			if(i!=0 && i<=numSortedDocuments){
				String currentDocument = sortedDocuments.get(i-1);
				if(relevantDocuments.containsKey(currentDocument)){
					rel=1.0;
					currentNumRelDocs++;
				}
			}
			
			if(i!=0){
				double precisionAtI = (double)currentNumRelDocs/(double)i;
				accPrecision+=precisionAtI*rel;
			}
		}
		
		double result = accPrecision*100/numRelevantDocuments;
		return result;
	}

	@Override
	public double getRPrecision() {
		int currentNumRelDocs=0;
		int numRelevantDocuments = relevantDocuments.size();
		int numSortedDocuments = sortedDocuments.size();
		for(int i=1; i<=numRelevantDocuments; i++){
			String currentDocument = sortedDocuments.get(i-1);
			if(relevantDocuments.containsKey(currentDocument)){
				currentNumRelDocs++;
			}
		}
		return (double)currentNumRelDocs*100/(double)numRelevantDocuments;
	}
	
	@Override
	public double getAreaUnderROC() {
		int score = sortedDocuments.size();
		List<DataPoint> points = new ArrayList<DataPoint>();
		for (String doc : sortedDocuments) {
			points.add(new DataPoint(score--, relevantDocuments.containsKey(doc)));
		}
		try {
			return AUCComputation.computeAUC(points)*100;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		return -1;
	}

	@Override
	public double getAveragePrecisionStdDev() {
		return 0;
	}

	@Override
	public double getRPrecisionStdDev() {
		return 0;
	}
	
	@Override
	public double getAreaUnderROCStdDev() {
		return 0;
	}
	
	public List<String> getSortedDocuments(){
		return sortedDocuments;
	}
	
	public Map<String,Integer> getRelevantDocuments(){
		return relevantDocuments;
	}
}
