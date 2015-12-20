package edu.columbia.cs.ltrie.online.svm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import pt.utl.ist.online.learning.RankingOnlineAlgorithm;
import pt.utl.ist.online.learning.engines.LinearOnlineEngine;
import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.DataObject;
import pt.utl.ist.online.learning.utils.LRUCache;
import pt.utl.ist.online.learning.utils.Pair;
import edu.columbia.cs.ltrie.RankingModel;
import edu.columbia.cs.ltrie.features.FeaturesCoordinator;

public class OnlineRankingModel implements RankingModel {

	private RankingOnlineAlgorithm<Long> algo;
	private FeaturesCoordinator coordinator;
	private long timeStamp=-1;
	private Random randomGenerator = new Random(31);
	private OnlineRankingModelSVMCache cache;
	private boolean isTempModel = false;

	private OnlineRankingModel(){
		
	}
	
	public OnlineRankingModel(FeaturesCoordinator coordinator, Collection<String> docs, Collection<String> relevantDocs,
			LinearOnlineEngine<Long> engine, int initialIterations) throws InvalidVectorIndexException{
		this.coordinator=coordinator;
		cache = new OnlineRankingModelSVMCache(coordinator);
		Map<Integer,DataObject<Map<Long,Double>>> relevantDocsFeatures = cache.getDocFeatures(relevantDocs);

		List<String> nonRelevantDocs = new ArrayList<String>(docs);
		nonRelevantDocs.removeAll(relevantDocs);
		Map<Integer,DataObject<Map<Long,Double>>> nonRelevantDocsFeatures = cache.getDocFeatures(nonRelevantDocs);

		algo = new RankingOnlineAlgorithm<Long>(relevantDocsFeatures, nonRelevantDocsFeatures, initialIterations, engine);
		cache.addRelevant(relevantDocsFeatures);
		cache.addNonRelevant(nonRelevantDocsFeatures);
	}
	
	private Map<Integer,String> getIds(Collection<String> docs){
		Map<Integer,String> results = new HashMap<Integer,String>();
		for(String doc : docs){
			results.put(cache.getId(doc),doc);
		}
		return results;
	}

	public void updateVectors(){
		if(algo!=null && coordinator.getTimeStamp()!=timeStamp){
			Map<String,Map<Long,Double>> vectors = new HashMap<String, Map<Long,Double>>();
			List<Pair<Integer,Map<Long,Double>>> cachedVectors = cache.getCachedVectors();
			int size = cachedVectors.size();
			for(int i=0; i<size; i++){
				Pair<Integer,Map<Long,Double>> pair = cachedVectors.get(i);
				vectors.put(cache.getDoc(pair.first()), pair.second());
			}
			timeStamp=coordinator.updateVectors(timeStamp, vectors);
		}
	}

	public void updateModel(Collection<String> currentBatchDocs, Collection<String> currentBatchRelDocs, int numIterations){
		updateVectors();
		Map<Integer,String> relevantDocsFeatures = getIds(currentBatchRelDocs);
		List<String> nonRelevantDocs = new ArrayList<String>(currentBatchDocs);
		nonRelevantDocs.removeAll(currentBatchRelDocs);
		Map<Integer,String> nonRelevantDocsFeatures = getIds(nonRelevantDocs);

		updateModel(relevantDocsFeatures, nonRelevantDocsFeatures, numIterations);
	}
	
	private void updateModel(Map<Integer,String> relevantVectors,
			Map<Integer,String> nonRelevantVectors,
			int numIterations){
		//System.out.println("Adding to history");
		//this.relevantElements.putAll(relevantVectors);
		//this.nonRelevantElements.putAll(nonRelevantVectors);

		if(!isTempModel){
			System.out.println("Creating relevant pairs");
		}
		List<Pair<Integer,Integer>> indexesRel = new ArrayList<Pair<Integer,Integer>>();
		List<Integer> rel1 = new ArrayList<Integer>(relevantVectors.keySet());
		int sizeRel1 = rel1.size();
		List<Integer> nRel1 = new ArrayList<Integer>(cache.getNonRelevantIds());
		nRel1.addAll(nonRelevantVectors.keySet());
		int sizeNRel1 = nRel1.size();
		if(sizeRel1!=0 && sizeNRel1!=0){
			for(int i=0; i<numIterations; i++){
				int indexRel=randomGenerator.nextInt(sizeRel1);
				int indexNRel=randomGenerator.nextInt(sizeNRel1);
				indexesRel.add(new Pair<Integer, Integer>(rel1.get(indexRel), nRel1.get(indexNRel)));
			}
		}

		if(!isTempModel){
			System.out.println("Creating non relevant pairs");
		}
		List<Pair<Integer,Integer>> indexesNRel = new ArrayList<Pair<Integer,Integer>>();
		List<Integer> rel2 = new ArrayList<Integer>(cache.getRelevantIds());
		rel2.addAll(relevantVectors.keySet());
		int sizeRel2 = rel2.size();
		List<Integer> nRel2 = new ArrayList<Integer>(nonRelevantVectors.keySet());
		int sizeNRel2 = nRel2.size();
		if(sizeRel2!=0 && sizeNRel2!=0){
			for(int i=0; i<numIterations; i++){
				int indexRel=randomGenerator.nextInt(sizeRel2);
				int indexNRel=randomGenerator.nextInt(sizeNRel2);
				indexesNRel.add(new Pair<Integer, Integer>(rel2.get(indexRel), nRel2.get(indexNRel)));
			}
		}

		if(!isTempModel){
			System.out.println(numIterations + " " + indexesRel.size() + " " + indexesNRel.size() +
					" " + relevantVectors.size() + " " + nonRelevantVectors.size());
		}
		
		if(indexesRel.size()>indexesNRel.size()){
			List<Pair<Integer,Integer>> tmp = indexesRel;
			indexesRel=indexesNRel;
			indexesNRel=tmp;
		}

		int numRel=0;
		int numNRel=0;

		if(!isTempModel){
			System.out.println("Updating");
		}
		int indexesSize=indexesRel.size();
		int currentIteration=0;
		int numUpdates=0;
		for(; currentIteration<Math.min(numIterations/2,indexesSize); currentIteration++){
			Pair<Integer,Integer> index = indexesRel.get(currentIteration);
			
			DataObject<Map<Long,Double>> rel = cache.getRelevantVector(index.first());
			DataObject<Map<Long,Double>> nonRel = cache.getNonRelevantVector(index.second());
			algo.updateModel(rel,nonRel);
			numUpdates++;
		}
		numIterations-=currentIteration;
		currentIteration=0;
		indexesSize=indexesNRel.size();
		for(;currentIteration<Math.min(numIterations,indexesSize); currentIteration++ ){
			Pair<Integer,Integer> index = indexesNRel.get(currentIteration);
			DataObject<Map<Long,Double>> rel = cache.getRelevantVector(index.first());
			DataObject<Map<Long,Double>> nonRel = cache.getNonRelevantVector(index.second());
			algo.updateModel(rel,nonRel);
			numUpdates++;
		}
		
		if(!isTempModel){
			System.out.println(numUpdates + " updates!");
			cache.cleanTmpCaches();
		}
	}

	public Map<Long,Double> getWeightVector(){
		return algo.getWeightVector();
	}

	public double getRho() {
		return algo.getRho();
	}
	
	public Map<Query,Double> getQueryScores() {
		Map<Long,Double> weights = algo.getWeightVector();
		Map<Query,Double> queryWeights = new HashMap<Query, Double>();
		for(Entry<Long,Double> entry : weights.entrySet()){
			Query q = coordinator.getQueries(entry.getKey());
			if(q==null){
				System.out.println("PROBLEM!!!!");
				System.exit(1);
			}
			queryWeights.put(q, entry.getValue());
		}
		
		return queryWeights;
	}
	
	public OnlineRankingModel getTempCopyModel(){
		OnlineRankingModel newModel = new OnlineRankingModel();
		newModel.algo=algo.copy();
		newModel.coordinator=coordinator;
		newModel.timeStamp=timeStamp;
		newModel.randomGenerator = randomGenerator;
		newModel.cache=cache;
		newModel.isTempModel = true;
		return newModel;
	}

	@Override
	public double getModelSimilarity(RankingModel copyModel) {
		return getCosineSimilarity(getWeightVector(), copyModel.getWeightVector());
	}
	
	private double getCosineSimilarity(Map<Long, Double> weightVector,
			Map<Long, Double> weightVector2) {
		Set<Long> commonKeys = new HashSet<Long>(weightVector.keySet());
		commonKeys.retainAll(weightVector2.keySet());
		
		double inner = 0;
		for(Long key : commonKeys){
			double val1 = weightVector.get(key);
			double val2 = weightVector2.get(key);
			inner+=val1*val2;
		}
		
		double normSq1 = 0;
		for(Entry<Long,Double> entry : weightVector.entrySet()){
			normSq1+=entry.getValue()*entry.getValue();
		}
		
		double normSq2 = 0;
		for(Entry<Long,Double> entry : weightVector2.entrySet()){
			normSq2+=entry.getValue()*entry.getValue();
		}
		
		return inner/Math.sqrt(normSq1*normSq2);
	}
}
