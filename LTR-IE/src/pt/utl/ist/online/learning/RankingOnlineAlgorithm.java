package pt.utl.ist.online.learning;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import pt.utl.ist.online.learning.engines.LinearOnlineEngine;
import pt.utl.ist.online.learning.engines.OnlineEngine;
import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.DataObject;
import pt.utl.ist.online.learning.utils.LRUCache;
import pt.utl.ist.online.learning.utils.Pair;


public class RankingOnlineAlgorithm<E> implements Serializable {

	private LinearOnlineEngine<E> engine;

	private Random randomGenerator = new Random(31);
	
	private RankingOnlineAlgorithm(){
		
	}

	public RankingOnlineAlgorithm(Map<Integer,DataObject<Map<E,Double>>> relevantVectors,
			Map<Integer,DataObject<Map<E,Double>>> nonRelevantVectors,
			int numIterations, LinearOnlineEngine<E> engine) throws InvalidVectorIndexException{
		this(relevantVectors, nonRelevantVectors, numIterations, engine, new Random(31));
	}

	public RankingOnlineAlgorithm(Map<Integer,DataObject<Map<E,Double>>> relevantVectors,
			Map<Integer,DataObject<Map<E,Double>>> nonRelevantVectors,
			int numIterations, LinearOnlineEngine<E> engine,
			Random randomGenerator) throws InvalidVectorIndexException{	
		this.randomGenerator=randomGenerator;
		this.engine = engine;

		engine.start();

		List<Pair<Integer,Integer>> indexes = new ArrayList<Pair<Integer,Integer>>();
		for(Integer keyRel : relevantVectors.keySet()){
			for(Integer keyNonRel : nonRelevantVectors.keySet()){
				indexes.add(new Pair<Integer, Integer>(keyRel, keyNonRel));
			}
		}

		Collections.shuffle(indexes, randomGenerator);

		int indexesSize=indexes.size();
		for(int currentIteration=0; currentIteration<Math.min(numIterations,indexesSize); currentIteration++){
			Pair<Integer,Integer> index = indexes.get(currentIteration);
			DataObject<Map<E,Double>> rel = relevantVectors.get(index.first());
			DataObject<Map<E,Double>> nonRel = nonRelevantVectors.get(index.second());
			DataObject<Map<E,Double>> inputVector = subtract(rel.getData(), nonRel.getData());
			engine.updateModel(inputVector, true);


			if((currentIteration+1)%1000==0){
				if((currentIteration+1)%10000==0 && currentIteration!=0){
					System.out.println(".");
				}else{
					System.out.print(".");
				}
			}

			//engine.objectiveReport(trainingData, labels);
			//System.out.println(labels.get(data.get(data.size()-1).getKey()));
		}

		engine.compileModel();
	}

	public void setRandomGenerator(Random r){
		this.randomGenerator=r;
	}

	private DataObject<Map<E,Double>> subtract(Map<E,Double> x, Map<E,Double> y){
		Map<E,Double> result = new HashMap<E, Double>(x);
		for(Entry<E,Double> entry : y.entrySet()){
			E key = entry.getKey();
			Double current = result.get(key);
			if(current==null){
				current=0.0;
			}
			double newVal = current-entry.getValue();
			if(newVal!=0){
				result.put(key, newVal);
			}else{
				result.remove(key);
			}
		}
		return new DataObject<Map<E,Double>>(result, 0);
	}

	public boolean classify(DataObject<Map<E,Double>> vector) throws InvalidVectorIndexException{
		return engine.getResult(vector);
	}

	public double getScore(DataObject<Map<E,Double>> vector){
		double value = engine.getScore(vector);
		return value;
	}
	
	public void updateModel(DataObject<Map<E,Double>> rel, DataObject<Map<E,Double>> nonRel){
		DataObject<Map<E,Double>> inputVector = subtract(rel.getData(), nonRel.getData());
		engine.updateModel(inputVector, true);
	}

	public Map<E,Double> getWeightVector() {
		return engine.getWeightVector();
	}

	public double getRho() {
		return engine.getRho();
	}

	public RankingOnlineAlgorithm<E> copy() {
		RankingOnlineAlgorithm<E> newModel = new RankingOnlineAlgorithm<E>();
		newModel.engine=engine.copy();
		newModel.randomGenerator=randomGenerator;
		return newModel;
	}
}
