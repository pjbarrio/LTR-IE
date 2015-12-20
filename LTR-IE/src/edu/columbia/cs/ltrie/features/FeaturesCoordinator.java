package edu.columbia.cs.ltrie.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.search.Query;

import pt.utl.ist.online.learning.utils.MemoryEfficientHashMap;
import pt.utl.ist.online.learning.utils.Pair;

import edu.columbia.cs.ltrie.datamodel.DocumentWithFields;

public class FeaturesCoordinator {
	private long currentId=1;
	private List<FeatureExtractor> extractors = new ArrayList<FeatureExtractor>();
	private List<FeatureExtractor> newExtractors = new ArrayList<FeatureExtractor>();
	private Map<String,Long> featureMap = new MemoryEfficientHashMap<String, Long>();
	public static Map<Long,Pair<String,String>> terms = new MemoryEfficientHashMap<Long, Pair<String,String>>();
	private Map<Long,Query> queries = new MemoryEfficientHashMap<Long, Query>();
	private long timeStamp=0;
	
	public void addFeatureExtractor(FeatureExtractor extractor){
		extractors.add(extractor);
		newExtractors.add(extractor);
		timeStamp++;
	}
	
	
	public Map<Long,Double> getFeatures(String doc){
		Map<Long,Double> features = new MemoryEfficientHashMap<Long, Double>();
		for(FeatureExtractor extractor : extractors){
			Map<String,Double> rawFeatures = extractor.extractFeatures(doc);
			for(Entry<String,Double> entry : rawFeatures.entrySet()){
				Long featId = featureMap.get(entry.getKey());
				if(featId==null){
					featId=currentId++;
					featureMap.put(entry.getKey(), featId);
					Pair<String,String> term = extractor.getTerm(entry.getKey());
					if(term!=null){
						terms.put(featId, term);
					}
					
					Query q = extractor.getQuery(entry.getKey());
					if(q!=null){
						queries.put(featId, q);
					}
				}
				features.put(featId, entry.getValue());
			}
		}
		return features;
	}
	
	public long updateVectors(long timeStamp, Map<String,Map<Long,Double>> vectors){
		if(this.timeStamp>timeStamp){
			for(Entry<String,Map<Long,Double>> vector : vectors.entrySet()){
				for(FeatureExtractor extractor : newExtractors){
					Map<String,Double> rawFeatures = extractor.extractFeatures(vector.getKey());
					for(Entry<String,Double> entry : rawFeatures.entrySet()){
						Long featId = featureMap.get(entry.getKey());
						if(featId==null){
							featId=currentId++;
							featureMap.put(entry.getKey(), featId);
							Pair<String,String> term = extractor.getTerm(entry.getKey());
							if(term!=null){
								terms.put(featId, term);
							}
							Query q = extractor.getQuery(entry.getKey());
							if(q!=null){
								queries.put(featId, q);
							}
						}
						vector.getValue().put(featId, entry.getValue().doubleValue());
					}
				}
			}
			newExtractors=new ArrayList<FeatureExtractor>();
		}
		return this.timeStamp;
	}
	
	public long getCurrentNumberFeatures(){
		return currentId;
	}

	public Pair<String, String> getTerm(Long key) {
		return terms.get(key);
	}
	
	public Query getQueries(Long key) {
		return queries.get(key);
	}
	
	public long getTimeStamp(){
		return timeStamp;
	}
}
