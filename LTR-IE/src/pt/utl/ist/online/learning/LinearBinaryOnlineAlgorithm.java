package pt.utl.ist.online.learning;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import pt.utl.ist.online.learning.engines.KernelElasticPegasosEngine;
import pt.utl.ist.online.learning.engines.KernelL1NormalizationPegasosEngine;
import pt.utl.ist.online.learning.engines.KernelPegasosEngine;
import pt.utl.ist.online.learning.engines.KernelPerceptronEngine;
import pt.utl.ist.online.learning.engines.LinearOnlineEngine;
import pt.utl.ist.online.learning.engines.LinearPegasosEngine;
import pt.utl.ist.online.learning.engines.OnlineEngine;
import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.kernels.Kernel;
import pt.utl.ist.online.learning.kernels.PolynomialKernel;
import pt.utl.ist.online.learning.utils.AccuracyComputation;
import pt.utl.ist.online.learning.utils.DataObject;
import pt.utl.ist.online.learning.utils.LibSVMStyleDatasetLoader;
import pt.utl.ist.online.learning.utils.Pair;


public class LinearBinaryOnlineAlgorithm<E> implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private LinearOnlineEngine<E> engine;
	private Random randomGenerator = new Random(11);
	
	private LinearBinaryOnlineAlgorithm(){
		
	}
	
	public LinearBinaryOnlineAlgorithm(Map<Integer,DataObject<Map<E,Double>>> trainingData, Map<Integer,Boolean> labels, int numEpochs, LinearOnlineEngine<E> engine) throws InvalidVectorIndexException{		
		this.engine = engine;
		trainingData=convertTrainingData(trainingData);
		
		engine.start();
		for(int currentEpoch=1; currentEpoch<=numEpochs; currentEpoch++){
			int updatesCount=0;
			List<Entry<Integer,DataObject<Map<E,Double>>>> data = new ArrayList<Entry<Integer,DataObject<Map<E,Double>>>>(trainingData.entrySet());
			Collections.shuffle(data,randomGenerator);
			for(Entry<Integer,DataObject<Map<E,Double>>> entry : data){
				DataObject<Map<E,Double>> inputVector = entry.getValue();
				Boolean desiredOutput = labels.get(entry.getKey());
				boolean updated = engine.updateModel(inputVector, desiredOutput);
				if(updated){
					updatesCount++;
				}
			}
			if(updatesCount==0){
				engine.objectiveReport(trainingData, labels);
				break;
			}
			
			if(currentEpoch%10==0){
				if(currentEpoch%100==0){
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

	public boolean classify(DataObject<Map<E,Double>> vector) throws InvalidVectorIndexException{
		vector = engine.convertVector(vector);
		return engine.getResult(vector);
	}
	
	public double getConfidence(String id, DataObject<Map<E,Double>> vector) throws InvalidVectorIndexException{
		
		vector = engine.convertVector(vector);
		return engine.getScore(vector);
	}
	
	public void addExampleAndUpdate(DataObject<Map<E,Double>> inputVector, boolean desiredOutput) throws InvalidVectorIndexException{
		inputVector = engine.convertVector(inputVector);
		engine.updateModel(inputVector, desiredOutput);
	}
	
	private Map<Integer,DataObject<Map<E,Double>>> convertTrainingData(Map<Integer,DataObject<Map<E,Double>>> trainingData) throws InvalidVectorIndexException{
		Map<Integer,DataObject<Map<E,Double>>> result = new HashMap<Integer,DataObject<Map<E,Double>>>(trainingData.size());
		
		for(Entry<Integer,DataObject<Map<E,Double>>> entry : trainingData.entrySet()){
			result.put(entry.getKey(), engine.convertVector(entry.getValue()));
		}
		
		return result;
	}
	
	public Map<E, Double> getWeightVectors() {
		return engine.getWeightVector();
				
	}
	
	public double getRho(){
		return engine.getRho();
	}

	public LinearBinaryOnlineAlgorithm<E> copy() {
		LinearBinaryOnlineAlgorithm<E> result = new LinearBinaryOnlineAlgorithm<E>();
		result.engine=engine.copy();
		result.randomGenerator=randomGenerator;
		return result;
	}
}