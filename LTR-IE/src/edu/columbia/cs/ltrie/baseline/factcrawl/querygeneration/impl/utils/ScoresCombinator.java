package edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.utils;

import java.util.Map;

public abstract class ScoresCombinator<T> {

	public abstract void initialize();

	public abstract void addFeaturesMap(Map<T, Double> features);

	public abstract Map<T,Double> obtainFeaturesMap();

}
