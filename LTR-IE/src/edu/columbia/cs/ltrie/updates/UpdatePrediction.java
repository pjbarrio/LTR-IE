package edu.columbia.cs.ltrie.updates;

import java.util.List;

public interface UpdatePrediction {
	boolean predictUpdate(List<String> docs, int docBeingProcessed);
	void performUpdate(List<String> docs, List<String> relevantDocs);
	String report();

}
