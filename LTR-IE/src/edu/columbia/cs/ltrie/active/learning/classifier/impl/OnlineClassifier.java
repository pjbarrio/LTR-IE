package edu.columbia.cs.ltrie.active.learning.classifier.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.lucene.search.Query;

import com.google.gdata.util.common.base.Pair;

import pt.utl.ist.online.learning.BinaryOnlineAlgorithm;
import pt.utl.ist.online.learning.LinearBinaryOnlineAlgorithm;
import pt.utl.ist.online.learning.utils.DataObject;
import edu.columbia.cs.ltrie.active.learning.classifier.ALTextClassifier;
import edu.columbia.cs.ltrie.active.learning.classifier.util.Combiner;
import edu.columbia.cs.ltrie.active.learning.classifier.util.impl.SumCombiner;
import edu.columbia.cs.ltrie.active.learning.data.Data;
import edu.columbia.cs.ltrie.active.learning.data.impl.OnlineLearningData;
import edu.columbia.cs.ltrie.features.FeaturesCoordinator;
import edu.columbia.cs.ltrie.indexing.IndexConnector;
import edu.columbia.cs.ltrie.indexing.SimpleBooleanSimilarity;

public class OnlineClassifier
extends
ALTextClassifier<LinearBinaryOnlineAlgorithm<Long>, Pair<Map<Integer, DataObject<Map<Long, Double>>>,Map<Integer, Boolean>>,Pair<DataObject<Map<Long, Double>>,Boolean>> {

	private FeaturesCoordinator coordinator;
	private Combiner combiner;
	private Map<String, Double> prevScoresCollection = new HashMap<String, Double>();
	private static HashMap<Integer, Map<String, Double>> cachedOld = null;

	public OnlineClassifier(FeaturesCoordinator coordinator) {
		this.coordinator = coordinator;
	}

	public OnlineClassifier(
			FeaturesCoordinator coordinator,
			List<Pair<Set<Long>,LinearBinaryOnlineAlgorithm<Long>>> classifiers,
			Combiner combiner) throws Exception {
		super(classifiers);

		if (cachedOld == null){
			cachedOld = new HashMap<Integer, Map<String,Double>>();
			for (int i = 0; i < classifiers.size(); i++) {
				cachedOld.put(i,new HashMap<String,Double>());
			}
		}

		this.coordinator = coordinator;
		this.combiner = combiner;
	}

	@Override
	public Data<Pair<Map<Integer, DataObject<Map<Long, Double>>>, Map<Integer, Boolean>>, Pair<DataObject<Map<Long, Double>>,Boolean>> createDataStructure(
			String[] label, List<String[]> text) throws IOException, Exception {

		Map<Integer, DataObject<Map<Long, Double>>> m1 = new HashMap<Integer, DataObject<Map<Long,Double>>>();

		Map<Integer, Boolean> m2 = new HashMap<Integer, Boolean>();

		int id = 0;

		for (int i = 0; i < label.length; i++) {

			String[] docs = text.get(i);

			for (int j = 0; j < docs.length; j++) {

				m2.put(id, label[i].equals("1"));

				m1.put(id, new DataObject<Map<Long,Double>>(coordinator.getFeatures(docs[j]), id));

				id++;

			}

		}

		return new OnlineLearningData(new Pair<Map<Integer, DataObject<Map<Long, Double>>>, Map<Integer, Boolean>>(m1,m2));

	}

	@Override
	public double getConfidenceValue(String text, String label)
			throws Exception {

		Map<Long,Double> map = null;

		DataObject<Map<Long, Double>> obj = null;

		double[] vals = new double[classifiers.size()];

		for (int j = 0; j < classifiers.size(); j++) {

			Set<Long> s = classifiers.get(j).getFirst();

			boolean takeOld = true;

			if (s == null){ //first time

				if (map == null)
					map = coordinator.getFeatures(text);

				if (obj == null)
					obj = new DataObject<Map<Long,Double>>(map, 0);

				vals[j] = (1.0d / (1.0 + Math.pow(Math.E, (-1.0) * classifiers.get(j).getSecond().getConfidence(text, obj))));
				takeOld = false;
				cachedOld.get(j).put(text,vals[j]);

			} else {

				if (map == null)
					map = coordinator.getFeatures(text);

				for (Long feat : map.keySet()) {

					if (s.contains(feat)){

						if (obj == null)
							obj = new DataObject<Map<Long,Double>>(map, 0);

						vals[j] = (1.0d / (1.0 + Math.pow(Math.E, (-1.0) * classifiers.get(j).getSecond().getConfidence(text, obj))));
						takeOld = false;
						cachedOld.get(j).put(text,vals[j]);
						break;
					}

				}

			}

			if (takeOld){
				vals[j] = cachedOld.get(j).get(text);
			}
		}

		return combiner.calculateCombination(vals);


	}

	@Override
	public Map<String, Double> getScores(Set<String> collection,
			IndexConnector conn, boolean independent) throws IOException {

		if (!independent){
		
			Map<Query,Double> queryWeights = new HashMap<Query, Double>();
	
			for (int i = 0; i < classifiers.size(); i++) {
	
				Map<Long,Double> weights = classifiers.get(i).getSecond().getWeightVectors();
	
				for(Entry<Long,Double> entry : weights.entrySet()){
					Query q = coordinator.getQueries(entry.getKey());
					if(q==null){
						System.out.println("PROBLEM!!!!");
						System.exit(1);
					}
	
					Double d = queryWeights.get(entry.getKey());
	
					if (d == null)
						d = 0.0;
	
					queryWeights.put(q, d + entry.getValue());
	
				}
	
			}
	
			Map<String,Float> newScoresCollection = conn.getScores(queryWeights, new SimpleBooleanSimilarity(), collection,false);
	
			Map<String,Double> ret = new HashMap<String, Double>();
	
			for (String string : collection) {
				
				Double v = (double)newScoresCollection.get(string);
				
				if (v == null){
					v = prevScoresCollection.get(string);
					if (v == null)
						v=0.0;
				}
				ret.put(string, v);
			}
			
			prevScoresCollection = ret;
			
			return ret;

		}else{
			
			Map<String,double[]> ret = new HashMap<String, double[]>();
			
			for (String string : collection) {
				ret.put(string, new double[classifiers.size()]);
			}
			
			for (int i = 0; i < classifiers.size(); i++) {
				
				Map<Long,Double> weights = classifiers.get(i).getSecond().getWeightVectors();

				Map<Query,Double> queryWeights = new HashMap<Query, Double>();
				
				for(Entry<Long,Double> entry : weights.entrySet()){
					Query q = coordinator.getQueries(entry.getKey());
					if(q==null){
						System.out.println("PROBLEM!!!!");
						System.exit(1);
					}
	
					queryWeights.put(q,entry.getValue());
	
				}
	
				Map<String,Float> newScoresCollection = conn.getScores(queryWeights, new SimpleBooleanSimilarity(), collection,false);
				
				for (String string : collection) {
					
					Float f = newScoresCollection.get(string);
					
					if (f != null)
						ret.get(string)[i] = (1.0d / (1.0 + Math.pow(Math.E, (-1.0) * f)));
							
				}
				
			}
			
			Map<String,Double> reto = new HashMap<String, Double>();
			
			for (String string : collection) {
				
				reto.put(string, combiner.calculateCombination(ret.get(string)));
				
			}
			
			return reto;
		}
	}
	
	public Map<Long,Double> getWeightVectors(boolean independent){
		if (!independent){
			Map<Long,Double>[] weights = new Map[classifiers.size()];
			for (int i = 0; i < classifiers.size(); i++) {
				weights[i] = classifiers.get(i).getSecond().getWeightVectors();
			}
			return new SumCombiner().getWeightVector(weights);
		}else{
			Map<Long,Double>[] weights = new Map[classifiers.size()];
			for (int i = 0; i < classifiers.size(); i++) {
				weights[i] = classifiers.get(i).getSecond().getWeightVectors();
			}
			return combiner.getWeightVector(weights);
		}
	}

	@Override
	public ALTextClassifier<LinearBinaryOnlineAlgorithm<Long>, Pair<Map<Integer, DataObject<Map<Long, Double>>>, Map<Integer, Boolean>>, Pair<DataObject<Map<Long, Double>>, Boolean>> copy() {
		
		try {
			
			List<Pair<Set<Long>, LinearBinaryOnlineAlgorithm<Long>>> cop = new ArrayList<Pair<Set<Long>,LinearBinaryOnlineAlgorithm<Long>>>(classifiers.size());
			
			for (Pair<Set<Long>, LinearBinaryOnlineAlgorithm<Long>> pair : classifiers) {
				
				cop.add(new Pair<Set<Long>, LinearBinaryOnlineAlgorithm<Long>>(pair.first == null ? null : new HashSet<Long>(pair.first), pair.second.copy()));
				
			}
			
			OnlineClassifier ret = new OnlineClassifier(coordinator, cop, combiner);
			
			if (prevScoresCollection != null)
				ret.prevScoresCollection = new HashMap<String, Double>(prevScoresCollection);
			
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return null;
	}

	public double getModelSimilarity(OnlineClassifier tctest) {
		double inner=0;
		double norm1=0;
		double norm2=0;
		Map<Long,Double>[] myWeights = new Map[classifiers.size()];
		for (int i = 0; i < classifiers.size(); i++) {
			myWeights[i] = classifiers.get(i).getSecond().getWeightVectors();
		}
		Map<Long,Double>[] otherWeights = new Map[classifiers.size()];
		for (int i = 0; i < tctest.classifiers.size(); i++) {
			otherWeights[i] = tctest.classifiers.get(i).getSecond().getWeightVectors();
		}
		
		for (int i = 0; i < classifiers.size(); i++) {
			inner+=getInnerProduct(myWeights[i], otherWeights[i]);
			norm1+=getInnerProduct(myWeights[i], myWeights[i]);
			norm2+=getInnerProduct(otherWeights[i], otherWeights[i]);
		}		
		
		return inner/Math.sqrt(norm1*norm2);
	}
	
	private double getInnerProduct(Map<Long, Double> weightVector,
			Map<Long, Double> weightVector2) {
		Set<Long> commonKeys = new HashSet<Long>(weightVector.keySet());
		commonKeys.retainAll(weightVector2.keySet());
		
		double inner = 0;
		for(Long key : commonKeys){
			double val1 = weightVector.get(key);
			double val2 = weightVector2.get(key);
			inner+=val1*val2;
		}
		
		return inner;
	}

//	@Override
//	public Map<pt.utl.ist.online.learning.utils.Pair<String, String>, Double> getTermWeights(double divisor) {
//		
//		System.err.println("When I tried sum, it did not return any result");
//		
//		Map<Long,Double> fwv = new HashMap<Long, Double>();
//		
//		for (int i = 0; i < classifiers.size(); i++) {
//			
//			Map<Long,Double> weightVector = classifiers.get(i).getSecond().getWeightVectors();
//			System.out.println(weightVector.size() + " features.");
//			double rho = classifiers.get(i).getSecond().getRho();
//
//			for (Entry<Long,Double> entry : weightVector.entrySet()) {
//			
//				if (entry.getValue()>rho/divisor){
//					
//					Double d = fwv.get(entry.getKey());
//					
//					if (d == null || (entry.getValue() - rho/divisor) > d){
//						fwv.put(entry.getKey(), entry.getValue() - rho/divisor);
//					}
//					
//				}
//				
//			}
//			
//		}
//		
//		Map<pt.utl.ist.online.learning.utils.Pair<String,String>, Double> termWeights = new HashMap<pt.utl.ist.online.learning.utils.Pair<String,String>, Double>();
//		for(Entry<Long,Double> entry : fwv.entrySet()){
//			pt.utl.ist.online.learning.utils.Pair<String,String> term = coordinator.getTerm(entry.getKey());
//			if(term!=null){
//				termWeights.put(term, entry.getValue());
//			}
//		}
//		
//		return termWeights;
//	}

	
}

