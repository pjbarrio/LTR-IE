package pt.utl.ist.online.learning.utils;

import java.util.HashMap;
import java.util.Map;

public class UpdateStatistics extends Statistics {
	
	private static final long serialVersionUID = -5413553790524803038L;
	
	public final static String numberOfFeaturesBeforeKey = "NUMBER_FEATURES_BEFORE";
	public final static String numberOfFeaturesAfterKey = "NUMBER_FEATURES_AFTER";
	public final static String newFeaturesKey = "NEW_FEATURES";
	public final static String removedFeaturesKey = "REMOVED_FEATURES";
	public final static String changedFeaturesKey = "CHANGED_FEATURES";
		
	public UpdateStatistics(Map<String, Integer> results) {
		super(results);
	}
	
	public UpdateStatistics(Map<Long, Double> oldVector, Map<Long, Double> newVector) {
		super(getUpdateStatistics(oldVector, newVector));
	}
	
	private static Map<String, Integer> getUpdateStatistics(Map<Long, Double> oldVector, Map<Long, Double> newVector) {
		Map<String, Integer> results = new HashMap<String, Integer>();
		
		int numberOfFeaturesBefore = oldVector.size();
		int numberOfFeaturesAfter = newVector.size();
		int newFeatures = 0;
		int removedFeatures = 0;
		int changedFeatures = 0;
		
		for (Long oldFeature : oldVector.keySet()) {
			if (!newVector.containsKey(oldFeature)) {
				removedFeatures++;
			} else if (newVector.get(oldFeature)!=oldVector.get(oldFeature)) {
				changedFeatures++;
			}
		}
		
		for (Long newFeature : newVector.keySet()) {
			if (!oldVector.containsKey(newFeature)) {
				newFeatures++;
			}
		}
		
		results.put(numberOfFeaturesBeforeKey, numberOfFeaturesBefore);
		results.put(numberOfFeaturesAfterKey, numberOfFeaturesAfter);
		results.put(newFeaturesKey, newFeatures);
		results.put(removedFeaturesKey, removedFeatures);
		results.put(changedFeaturesKey, changedFeatures);
		
		return results;
	}
	
	public int getNumberOfFeaturesBefore() {
		return getResults().get(numberOfFeaturesBeforeKey);
	}
	
	public int getNumberOfFeaturesAfter() {
		return getResults().get(numberOfFeaturesAfterKey);
	}
	
	public int getNewFeatures() {
		return getResults().get(newFeaturesKey);
	}
	
	public int getRemovedFeatures() {
		return getResults().get(removedFeaturesKey);
	}
	
	public int getChangedFeatures() {
		return getResults().get(changedFeaturesKey);
	}
}
