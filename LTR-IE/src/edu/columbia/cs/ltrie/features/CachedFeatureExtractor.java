package edu.columbia.cs.ltrie.features;

import java.util.Map;

import org.apache.lucene.search.Query;

import pt.utl.ist.online.learning.utils.MemoryEfficientHashMap;
import pt.utl.ist.online.learning.utils.Pair;

public class CachedFeatureExtractor extends FeatureExtractor {

	private FeatureExtractor e;
	private Map<String, Map<String, Double>> cached;
	
	public CachedFeatureExtractor(FeatureExtractor e) {
		this.e = e;
		this.cached = new MemoryEfficientHashMap<String, Map<String,Double>>();
	}

	@Override
	public Map<String, Double> extractFeatures(String doc) {
		
		Map<String,Double> ret = cached.get(doc);
		
		if (ret == null){
			ret = e.extractFeatures(doc);
			cached.put(doc, ret);
		}
		
		return ret;
		
	}

	@Override
	public Pair<String, String> getTerm(String term) {
		return e.getTerm(term);
	}

	@Override
	public Query getQuery(String term) {
		return e.getQuery(term);
	}

}
