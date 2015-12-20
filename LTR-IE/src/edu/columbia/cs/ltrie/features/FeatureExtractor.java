package edu.columbia.cs.ltrie.features;

import java.util.Map;

import org.apache.lucene.search.Query;

import pt.utl.ist.online.learning.utils.Pair;

import edu.columbia.cs.ltrie.datamodel.DocumentWithFields;

public abstract class FeatureExtractor {
	public abstract Map<String,Double> extractFeatures(String doc);
	public abstract Pair<String,String> getTerm(String term);
	public abstract Query getQuery(String term);
}
