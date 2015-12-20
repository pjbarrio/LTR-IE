package edu.columbia.cs.ltrie.features.similarity;

import java.util.Map;
import java.util.Set;

public interface Similarity {
	public double computeSimilarity(Map<String,Set<String>> queryTerms, String doc);
	public String getLabel();
}
