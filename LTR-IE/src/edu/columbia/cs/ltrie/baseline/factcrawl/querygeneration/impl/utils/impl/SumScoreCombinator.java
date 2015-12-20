package edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.utils.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.utils.ScoresCombinator;

public class SumScoreCombinator<T> extends ScoresCombinator<T> {

	private Map<T, Double> finalMap;

	@Override
	public void initialize() {
		finalMap = new HashMap<T, Double>();
	}

	@Override
	public void addFeaturesMap(Map<T, Double> features) {
		for (Entry<T,Double> entry : features.entrySet()) {
			Double val = finalMap.remove(entry.getKey());
			if (val == null){
				val = 0.0;
			}
			finalMap.put(entry.getKey(), val + entry.getValue());
		}		
	}

	@Override
	public Map<T, Double> obtainFeaturesMap() {
		return finalMap;
	}

}
