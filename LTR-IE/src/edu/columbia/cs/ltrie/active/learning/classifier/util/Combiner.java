package edu.columbia.cs.ltrie.active.learning.classifier.util;

import java.util.Map;

public interface Combiner {

	public double calculateCombination(double[] values);
	public Map<Long,Double> getWeightVector(Map<Long,Double> ... weights);
	
}
