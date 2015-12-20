package edu.columbia.cs.ltrie.features;

import java.util.Map;

import org.apache.lucene.search.Query;

import pt.utl.ist.online.learning.utils.MemoryEfficientHashMap;
import pt.utl.ist.online.learning.utils.Pair;

public class CachedFeaturesCoordinator extends FeaturesCoordinator {

	Map<String,Map<Long,Double>> cached;
	
	public CachedFeaturesCoordinator() {
		cached = new MemoryEfficientHashMap<String, Map<Long,Double>>();
	}
	
	@Override
	public Map<Long, Double> getFeatures(String doc) {
		
		Map<Long,Double> ret = cached.get(doc);
		
		if (ret == null){
			
			ret = super.getFeatures(doc);
			cached.put(doc, ret);
			
		}
		
		return ret;
	}

	public void remove(String doc) {
		cached.remove(doc);		
	}
	
}
