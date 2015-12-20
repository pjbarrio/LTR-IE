package edu.columbia.cs.ltrie.updates;

import java.util.List;
import java.util.Set;

import prototype.CIMPLE.utils.CPUTimeMeasure;

public class ExactWindowUpdateDecision implements UpdateDecision {

	int windowSize;
	private long totalTime = 0;
	private int numDetections = 0;
	
	public ExactWindowUpdateDecision(int windowSize){
		this.windowSize=windowSize;
	}
	
	@Override
	public boolean doUpdate(List<String> docs, List<String> relevantDocs) {
		long start = CPUTimeMeasure.getCpuTime();
		boolean result = docs.size()==windowSize;
		long end = CPUTimeMeasure.getCpuTime();
		totalTime+=(end-start);
		numDetections++;
		return result;
	}

	@Override
	public void reset() {
		
	}
	
	@Override
	public String report() {
		return "" + ((double)totalTime/(double)numDetections)/1000000;
	}

}
