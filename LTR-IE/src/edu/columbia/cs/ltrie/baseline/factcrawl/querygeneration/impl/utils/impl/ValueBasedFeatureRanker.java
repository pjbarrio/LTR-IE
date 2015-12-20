package edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.utils.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.utils.FeaturesRanker;
import edu.columbia.cs.ltrie.baseline.factcrawl.utils.MapBasedComparator;

public class ValueBasedFeatureRanker<T> extends FeaturesRanker<T> {

	private boolean descending;

	public ValueBasedFeatureRanker(boolean descending){
		this.descending = descending;
	}
	
	@Override
	public List<T> rankFeatures(Map<T, Double> featuresMap) {
		
		List<T> feats = new ArrayList<T>(featuresMap.keySet());
		
		Collections.sort(feats, new MapBasedComparator<T>(featuresMap, descending));
		
		return feats;
	}

}
