package edu.columbia.cs.ltrie.updates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import prototype.CIMPLE.utils.CPUTimeMeasure;
import pt.utl.ist.online.learning.BinaryOnlineAlgorithm;
import pt.utl.ist.online.learning.engines.LinearPegasosEngine;
import pt.utl.ist.online.learning.engines.OnlineEngine;
import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.DataObject;

import com.google.gdata.util.common.base.Pair;

import edu.columbia.cs.ltrie.features.FeaturesCoordinator;

public class FeatureRankOnline implements UpdateDecision {

	private FeaturesCoordinator coordinator;
	private double threshold;
	private int numberOfFeatures;
	private BinaryOnlineAlgorithm<Map<Long, Double>> classifier;
	private int id;
	private Map<Long,Double> prevWeigths;
	private Map<Long, Double> currWeights;
	private int prevRelSize;
	private long totalTime = 0;
	private int numDetections = 0;
	
	public FeatureRankOnline(List<String> docs, List<String> relevantDocs, FeaturesCoordinator coordinator, double threshold, int numberOfFeatures) throws InvalidVectorIndexException{
		
		this.coordinator = coordinator;
		this.threshold = threshold;
		this.numberOfFeatures = numberOfFeatures;
		
		initializeClassifier(docs,relevantDocs);
		
	}
	
	private void initializeClassifier(List<String> docs,
			List<String> relevantDocs) throws InvalidVectorIndexException {
		
		double lambda = 0.5;
		double hingeParameter = 1;
		
		OnlineEngine<Map<Long,Double>> engine = new LinearPegasosEngine<Long>(lambda, hingeParameter);
		
		int numEpochs = 100;
		
		id = 0;
		
		prevRelSize = 0;
		
		Pair<Map<Integer,DataObject<Map<Long,Double>>>, Map<Integer,Boolean>> data = generateData(docs,relevantDocs);
		
		classifier = new BinaryOnlineAlgorithm<Map<Long,Double>>(data.getFirst(), data.getSecond(), numEpochs, engine);
		
		prevWeigths = getTop(classifier.getWeightVectors());
		
	}

	private Map<Long, Double> getTop(Map<Long, Double> weightVectors) {
		
		Top<Long> t = new Top<Long>(numberOfFeatures, weightVectors); 
		
		for (Entry<Long,Double> entry : weightVectors.entrySet()) {
			
			t.put(entry.getKey());
			
		}
		
		Map<Long,Double> ret = new HashMap<Long, Double>();
		
		for (Long element : t) {
			
			ret.put(element, weightVectors.get(element));
			
		}
		
		return ret;
	}

	private Pair<Map<Integer, DataObject<Map<Long, Double>>>, Map<Integer, Boolean>> generateData(
			List<String> docs, List<String> relevantDocs) {
		
		Map<Integer, DataObject<Map<Long, Double>>> m1 = new HashMap<Integer, DataObject<Map<Long,Double>>>();
		
		Map<Integer, Boolean> m2 = new HashMap<Integer, Boolean>();
		
		List<String> useless = new ArrayList<String>(docs);
		
		useless.removeAll(relevantDocs);
		
		for (int j = 0; j < useless.size(); j++) {
			
			m2.put(id, false);
			
			m1.put(id, new DataObject<Map<Long,Double>>(coordinator.getFeatures(useless.get(j)), id));
			
			id++;
			
		}

		for (int j = 0; j < relevantDocs.size(); j++) {
			
			m2.put(id, true);
			
			m1.put(id, new DataObject<Map<Long,Double>>(coordinator.getFeatures(relevantDocs.get(j)), id));
			
			id++;
			
		}
		
		return new Pair<Map<Integer, DataObject<Map<Long, Double>>>, Map<Integer, Boolean>>(m1,m2);
		
	}

	@Override
	public boolean doUpdate(List<String> docs, List<String> relevantDocs){
		
		try {
			long start = CPUTimeMeasure.getCpuTime();
			boolean pos = true;
			if (relevantDocs.size() == prevRelSize){
				pos = false;
			}
			prevRelSize = relevantDocs.size();
			updateClassifier(docs.get(docs.size()-1), pos);
			currWeights = classifier.getWeightVectors();
			boolean result =  calculateDistance(currWeights,prevWeigths) > threshold;
			long end = CPUTimeMeasure.getCpuTime();
			totalTime+=(end-start);
			numDetections++;
			return result;
		} catch (InvalidVectorIndexException e) {
			e.printStackTrace();
		}
		return true;
	}

	private double calculateDistance(Map<Long, Double> currWeights,
			Map<Long, Double> prevWeights) {
		
		double sum = 0;

		for (Entry<Long,Double> entry : prevWeights.entrySet()) {

			Double originalWeight = currWeights.get(entry.getKey());

			if (originalWeight == null){
				originalWeight = 0.0;
			}

			//The other strategy is to use the Generalized
			
			sum += Math.abs(originalWeight - entry.getValue());//*Math.abs(originalRank - entry.getValue().getFirst());

		}

		//if (id % 1000 == 0)
		//	System.err.println(sum);
		
		return sum;
		
	}

	private void updateClassifier(String doc,
			boolean pos) throws InvalidVectorIndexException {
		
		classifier.addExampleAndUpdate(generateOneData(doc), pos);
		
	}

	private DataObject<Map<Long, Double>> generateOneData(String doc) {
		
		DataObject<Map<Long, Double>> ret = new DataObject<Map<Long,Double>>(coordinator.getFeatures(doc),id);
		id++;
		return ret;
	}

	@Override
	public void reset() {
		
		prevRelSize = 0;
		
		prevWeigths.clear();
		
		prevWeigths = getTop(currWeights);

	}
	
	@Override
	public String report() {
		return "" + ((double)totalTime/(double)numDetections)/1000000;
	}

}
