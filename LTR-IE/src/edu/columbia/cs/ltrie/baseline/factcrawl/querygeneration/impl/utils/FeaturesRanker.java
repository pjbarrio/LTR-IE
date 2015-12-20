package edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.utils;

import java.util.List;
import java.util.Map;

public abstract class FeaturesRanker<T> {

	public abstract List<T> rankFeatures(Map<T, Double> featuresMap);

}
