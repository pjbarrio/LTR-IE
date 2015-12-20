package pt.utl.ist.online.learning.engines.oneclass;

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
import pt.utl.ist.online.learning.kernels.GaussianKernel;
import pt.utl.ist.online.learning.kernels.Kernel;
import pt.utl.ist.online.learning.kernels.PolynomialKernel;
import pt.utl.ist.online.learning.utils.AccuracyComputation;
import pt.utl.ist.online.learning.utils.DataObject;
import pt.utl.ist.online.learning.utils.LibSVMStyleDatasetLoader;
import pt.utl.ist.online.learning.utils.Pair;


public class OneClassOnlineAlgorithm<E> implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private OneClassEngine<E> engine;
	private Random randomGenerator = new Random(11);
	
	public OneClassOnlineAlgorithm(Map<Integer,DataObject<E>> trainingData, int numEpochs, OneClassEngine<E> engine) throws InvalidVectorIndexException{		
		this.engine = engine;
		
		engine.start();
		for(int currentEpoch=1; currentEpoch<=numEpochs; currentEpoch++){
			int updatesCount=0;
			int elem=0;
			List<Entry<Integer,DataObject<E>>> data = new ArrayList<Entry<Integer,DataObject<E>>>(trainingData.entrySet());
			Collections.shuffle(data,randomGenerator);
			for(Entry<Integer,DataObject<E>> entry : data){
				DataObject<E> inputVector = entry.getValue();
				boolean updated = engine.updateModel(inputVector);
				if(updated){
					updatesCount++;
				}
				elem++;
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

	public boolean classify(DataObject<E> vector) throws InvalidVectorIndexException{
		return engine.getResult(vector);
	}
		
	public double getConfidence(String id, DataObject<E> vector) throws InvalidVectorIndexException{
		return engine.getScore(vector);
	}
	
	public void addExampleAndUpdate(DataObject<E> inputVector, boolean desiredOutput) throws InvalidVectorIndexException{
		engine.updateModel(inputVector);
	}
	
	/*public static void main(String[] args) throws InvalidVectorIndexException {
		int fold=1;
		//Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>> datasetTrain = LibSVMStyleDatasetLoader.load("syntheticLinearTrainingPositive.txt");
		Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>> datasetTrain = LibSVMStyleDatasetLoader.load("syntheticTrainingData.txt");
		//Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>> datasetTrain = SpambaseLoader.load("spambase-f"+ fold +".data");
		//Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>> datasetTrain = PimaDiabetesLoader.load("pima-indians-diabetes-f"+ fold +".data");
		Map<Integer,DataObject<Map<Integer,Double>>> instancesTrain = datasetTrain.first();
		Map<Integer,Boolean> labelsTrain = datasetTrain.second();
		
		for(Entry<Integer,Boolean> entry : labelsTrain.entrySet()){
			if(!entry.getValue()){
				instancesTrain.remove(entry.getKey());
			}
		}
		
		System.out.println(instancesTrain.size());
		
		
		//OneClassEngine<Map<Integer,Double>> engine = new OneClassLinearPegasosEngine<Integer>(0.9);
		OneClassEngine<Map<Integer,Double>> engine = new OneClassKernelPegasosEngine<Map<Integer,Double>>(0.5, new GaussianKernel(0.01));
		
		OneClassOnlineAlgorithm<Map<Integer,Double>> p = new OneClassOnlineAlgorithm<Map<Integer,Double>>(instancesTrain, 100, engine);
		
		
		
		
		//Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>> datasetTest = LibSVMStyleDatasetLoader.load("syntheticLinearTesting.txt");
		Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>> datasetTest = LibSVMStyleDatasetLoader.load("syntheticTestingData.txt");
		Map<Integer,DataObject<Map<Integer,Double>>> instancesTest = datasetTest.first();
		Map<Integer,Boolean> labelsTest = datasetTest.second();
		System.out.println(AccuracyComputation.computeAccuracy(instancesTest, labelsTest, p));
	}*/

	public Map<Long, Double> getWeightVectors() {
		return (Map<Long, Double>) ((LinearPegasosEngine<E>)engine).getWeightVector();
				
	}
}