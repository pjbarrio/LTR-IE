package edu.columbia.cs.ltrie.updates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AveragePrecisionBasedUpdateDecision implements UpdateDecision {

	private int numRelevantCurrent=0;
	private int numAll=0;
	private List<Integer> rel = new ArrayList<Integer>();
	private List<Double> precisionPos = new ArrayList<Double>();
	private int totalPos;
	private int totalTotal;
	private double threshold;
	
	public AveragePrecisionBasedUpdateDecision(int initialPos, int initialTotal){
		this.totalPos=initialPos;
		this.totalTotal=initialTotal;
		this.threshold=((double)initialPos)/((double)initialTotal);
	}
	
	
	@Override
	public boolean doUpdate(List<String> docs, List<String> relevantDocs) {
		Set<String> currentRel = new HashSet<String>(relevantDocs);
		currentRel.retainAll(docs);
		int numRelevantDocs = currentRel.size();
		if(numRelevantCurrent!=numRelevantDocs){
			numRelevantCurrent++;
			rel.add(1);
		}else{
			//TODO
			rel.add(0);
		}
		
		//TODO
		numAll++;
		precisionPos.add((double)numRelevantCurrent/(double)numAll);
		double sum = 0.0;
		for(int j=0; j<rel.size(); j++){
			int r = rel.get(j);
			double p = precisionPos.get(j);
			sum+=(r*p);
		}
		
		double averagePrecision=Double.POSITIVE_INFINITY;
		if(numRelevantDocs!=0){
			averagePrecision=(sum/((double)numRelevantCurrent));
		}
				
		return averagePrecision<threshold || (threshold==0 && averagePrecision!=Double.POSITIVE_INFINITY);
	}

	@Override
	public void reset() {
		totalPos+=numRelevantCurrent;
		totalTotal+=numAll;
		this.threshold=((double)totalPos)/((double)totalTotal);
		numRelevantCurrent=0;
		numAll=0;
		rel = new ArrayList<Integer>();
		precisionPos = new ArrayList<Double>();
	}
	
	@Override
	public String report() {
		return "";
	}

}
