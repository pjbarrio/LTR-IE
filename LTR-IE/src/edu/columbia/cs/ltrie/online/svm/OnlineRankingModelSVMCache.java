package edu.columbia.cs.ltrie.online.svm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pt.utl.ist.online.learning.utils.DataObject;
import pt.utl.ist.online.learning.utils.LRUCache;
import pt.utl.ist.online.learning.utils.Pair;
import edu.columbia.cs.ltrie.features.FeaturesCoordinator;

public class OnlineRankingModelSVMCache {
	private FeaturesCoordinator coordinator;
	private Map<String,Integer> idsMap = new HashMap<String, Integer>();
	private Map<Integer,String> idsMapInverted = new HashMap<Integer,String>();
	private int idVecs = 0;
	private Map<Integer,DataObject<Map<Long,Double>>> relevantElements = new LRUCache<Integer,DataObject<Map<Long,Double>>>(5000);
	private Map<Integer,DataObject<Map<Long,Double>>> nonRelevantElements = new LRUCache<Integer,DataObject<Map<Long,Double>>>(5000);
	private Map<Integer,DataObject<Map<Long,Double>>> tempRelevantElements = new LRUCache<Integer,DataObject<Map<Long,Double>>>(5000);
	private Map<Integer,DataObject<Map<Long,Double>>> tempNonRelevantElements = new LRUCache<Integer,DataObject<Map<Long,Double>>>(5000);
	
	public OnlineRankingModelSVMCache(FeaturesCoordinator coord){
		coordinator=coord;
	}

	public void addRelevant(Map<Integer, DataObject<Map<Long, Double>>> relevantDocsFeatures) {
		relevantElements.putAll(relevantDocsFeatures);
	}
	
	public void addNonRelevant(Map<Integer, DataObject<Map<Long, Double>>> nonRelevantDocsFeatures) {
		nonRelevantElements.putAll(nonRelevantDocsFeatures);
	}
	
	public void addRelevant(Integer id, DataObject<Map<Long, Double>> relevantDocsFeatures) {
		relevantElements.put(id,relevantDocsFeatures);
	}
	
	public void addNonRelevant(Integer id, DataObject<Map<Long, Double>> nonRelevantDocsFeatures) {
		nonRelevantElements.put(id,nonRelevantDocsFeatures);
	}
	
	public List<Pair<Integer,Map<Long,Double>>> getCachedVectors(){
		//System.out.println(relevantElements.size() + " " + nonRelevantElements.size() + " " + tempRelevantElements.size() + " " + tempNonRelevantElements.size());
		List<Pair<Integer,Map<Long,Double>>> list = new ArrayList<Pair<Integer,Map<Long,Double>>>();

		for(Entry<Integer,DataObject<Map<Long,Double>>> entry : relevantElements.entrySet()){
			list.add(new Pair<Integer, Map<Long,Double>>(entry.getKey(), entry.getValue().getData()));
		}

		for(Entry<Integer,DataObject<Map<Long,Double>>> entry : nonRelevantElements.entrySet()){
			list.add(new Pair<Integer, Map<Long,Double>>(entry.getKey(), entry.getValue().getData()));
		}

		return list;
	}
	
	public Set<Integer> getRelevantIds(){
		return relevantElements.keySet();
	}
	
	public Set<Integer> getNonRelevantIds(){
		return nonRelevantElements.keySet();
	}
	
	public DataObject<Map<Long,Double>> getRelevantVector(Integer id){
		DataObject<Map<Long,Double>> result = relevantElements.get(id);
		if(result==null){
			result = tempRelevantElements.get(id);
			if(result==null){
				String doc = getDoc(id);
				DataObject<Map<Long,Double>> docFeatures = getDocFeatures(doc);
				result=docFeatures;
				tempRelevantElements.put(id, docFeatures);
			}
		}
		return result;
	}
	
	public DataObject<Map<Long,Double>> getNonRelevantVector(Integer id){
		DataObject<Map<Long,Double>> result = nonRelevantElements.get(id);
		if(result==null){
			result = tempNonRelevantElements.get(id);
			if(result==null){
				String doc = getDoc(id);
				DataObject<Map<Long,Double>> docFeatures = getDocFeatures(doc);
				result=docFeatures;
				tempNonRelevantElements.put(id, docFeatures);
			}
		}
		return result;
	}
	
	public Map<Integer,DataObject<Map<Long,Double>>> getDocFeatures(Collection<String> docs){
		Map<Integer,DataObject<Map<Long,Double>>> results = new HashMap<Integer,DataObject<Map<Long,Double>>>();
		for(String doc : docs){
			Integer id = getId(doc);
			DataObject<Map<Long,Double>> docFeatures = getDocFeatures(doc);
			results.put(id,docFeatures);
		}
		return results;
	}
	
	private DataObject<Map<Long,Double>> getDocFeatures(String doc){
		Map<Long,Double> rfeatures = coordinator.getFeatures(doc);
		DataObject<Map<Long,Double>> docFeatures = new DataObject<Map<Long,Double>>(rfeatures, 0);
		return docFeatures;
	}
	
	public void cleanTmpCaches(){
		relevantElements.putAll(tempRelevantElements);
		nonRelevantElements.putAll(tempNonRelevantElements);
		tempRelevantElements.clear();
		tempNonRelevantElements.clear();
	}
	
	public Integer getId(String doc) {
		Integer id = idsMap.get(doc);
		if(id==null){
			id=idVecs++;
			idsMap.put(doc, id);
			idsMapInverted.put(id, doc);
		}
		return id;
	}
	
	public String getDoc(Integer id) {
		String doc = idsMapInverted.get(id);
		return doc;
	}
}
