package edu.columbia.cs.ltrie.active.learning.classifier.util.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.columbia.cs.ltrie.active.learning.classifier.util.Combiner;

public class SumCombiner implements Combiner {

	@Override
	public double calculateCombination(double[] values) {
		
		double sum = 0.0;
		
		for (int i = 0; i < values.length; i++) {
			
			sum+=  values[i];
			
		}
		
		return sum;
		
	}

	@Override
	public Map<Long, Double> getWeightVector(Map<Long, Double>... weights) {
		Set<Long> keys = new HashSet<Long>();
		for(Map<Long,Double> weight : weights){
			keys.addAll(weight.keySet());
		}
		
		Map<Long,Double> result = new HashMap<Long,Double>();
		for(Long key : keys){
			double sum = 0.0;
			for(int i=0; i<weights.length; i++){
				Double val = weights[i].get(key);
				if(val!=null){
					sum+=val;
				}
			}
			result.put(key, sum);
		}
		return result;
	}


}
