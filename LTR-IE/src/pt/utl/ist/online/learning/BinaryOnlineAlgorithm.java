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


public class BinaryOnlineAlgorithm<E> implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private OnlineEngine<E> engine;
	private Random randomGenerator = new Random(11);
	
	public BinaryOnlineAlgorithm(Map<Integer,DataObject<E>> trainingData, Map<Integer,Boolean> labels, int numEpochs, OnlineEngine<E> engine) throws InvalidVectorIndexException{		
		this.engine = engine;
		trainingData=convertTrainingData(trainingData);
		
		engine.start();
		for(int currentEpoch=1; currentEpoch<=numEpochs; currentEpoch++){
			int updatesCount=0;
			int elem=0;
			List<Entry<Integer,DataObject<E>>> data = new ArrayList<Entry<Integer,DataObject<E>>>(trainingData.entrySet());
			Collections.shuffle(data,randomGenerator);
			for(Entry<Integer,DataObject<E>> entry : data){
				DataObject<E> inputVector = entry.getValue();
				Boolean desiredOutput = labels.get(entry.getKey());
				boolean updated = engine.updateModel(inputVector, desiredOutput);
				if(updated){
					updatesCount++;
				}
				elem++;
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

	public boolean classify(DataObject<E> vector) throws InvalidVectorIndexException{
		vector = engine.convertVector(vector);
		return engine.getResult(vector);
	}
	
	public double getConfidence(String id, DataObject<E> vector) throws InvalidVectorIndexException{
		
		vector = engine.convertVector(vector);
		return engine.getScore(vector);
	}
	
	public void addExampleAndUpdate(DataObject<E> inputVector, boolean desiredOutput) throws InvalidVectorIndexException{
		inputVector = engine.convertVector(inputVector);
		engine.updateModel(inputVector, desiredOutput);
	}
	
	private Map<Integer,DataObject<E>> convertTrainingData(Map<Integer,DataObject<E>> trainingData) throws InvalidVectorIndexException{
		Map<Integer,DataObject<E>> result = new HashMap<Integer, DataObject<E>>();
		
		for(Entry<Integer,DataObject<E>> entry : trainingData.entrySet()){
			result.put(entry.getKey(), engine.convertVector(entry.getValue()));
		}
		
		return result;
	}
	
	public static void main(String[] args) throws InvalidVectorIndexException {
		int fold=1;
		//Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>> datasetTrain = LibSVMStyleDatasetLoader.load("syntheticLinearTraining.txt");
		Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>> datasetTrain = LibSVMStyleDatasetLoader.load("syntheticTrainingData.txt");
		//Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>> datasetTrain = SpambaseLoader.load("spambase-f"+ fold +".data");
		//Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>> datasetTrain = PimaDiabetesLoader.load("pima-indians-diabetes-f"+ fold +".data");
		Map<Integer,DataObject<Map<Integer,Double>>> instancesTrain = datasetTrain.first();
		Map<Integer,Boolean> labelsTrain = datasetTrain.second();
		
		Kernel<Map<Integer,Double>> k = new PolynomialKernel(2);
		//Kernel<Map<Integer,Double>> k = new GaussianKernel(0.001);
		//Kernel<Map<Integer,Double>> k = new LinearKernel();
		//OnlineEngine<Map<Integer,Double>> engine = new LinearPerceptronEngine(0.5);
		//OnlineEngine<Map<Integer,Double>> engine = new KernelPerceptronEngine<Map<Integer,Double>>(0.5,k);
		//OnlineEngine<Map<Integer,Double>> engine = new NewKernelL1StochasticGradientDescentEngine(new UniformLearningRate(0.5), 1, instancesTrain.size(), 1, k);
		//OnlineEngine<Map<Integer,Double>> engine = new KernelSVMStochasticGradientDescentEngine(new UniformLearningRate(0.01), 0.5, k, instancesTrain.values());
		//OnlineEngine<Map<Integer,Double>> engine = new NormaEngine<Map<Integer,Double>>(1.0/2000, 1000, 0.005, new SoftMarginLoss(1.0), k);
		OnlineEngine<Map<Integer,Double>> engine = new LinearPegasosEngine(1.0/2000.0, 1);
		//OnlineEngine<Map<Integer,Double>> engine = new KernelPegasosEngine<Map<Integer,Double>>(1.0/2000.0, 1, k);
		//OnlineEngine<Map<Integer,Double>> engine = new KernelL1NormalizationPegasosEngine<Map<Integer,Double>>(1/2000.0, 1, k);
		//OnlineEngine<Map<Integer,Double>> engine = new KernelElasticPegasosEngine<Map<Integer,Double>>(1/2000.0, 0.9 , 1, k);
		
		//PerceptronWikipedia<Map<Integer,Double>> p = new PerceptronWikipedia<Map<Integer,Double>>(instances, labels,100, new LinearEngine(0.5));
		//OnlineAlgorithm<Map<Integer,Double>> p = new OnlineAlgorithm<Map<Integer,Double>>(instances, labels, 0.5,1000, new GaussianKernel(0.001));
		//OnlineAlgorithm<Map<Integer,Double>> p = new OnlineAlgorithm<Map<Integer,Double>>(instances, labels,10000, new LinearPerceptronEngine(1));
		//OnlineAlgorithm<Map<Integer,Double>> p = new OnlineAlgorithm<Map<Integer,Double>>(instances, labels,10000, new LinearPassiveAgressiveEngine(new SimplePassiveAggressiveLearningRate()));
		//OnlineAlgorithm<Map<Integer,Double>> p = new OnlineAlgorithm<Map<Integer,Double>>(instances, labels,1000, new KernelPassiveAggressiveEngine(new PAIIPassiveAggressiveLearningRate(0.01), new GaussianKernel(0.001)));
		//OnlineAlgorithm<Map<Integer,Double>> p = new OnlineAlgorithm<Map<Integer,Double>>(instances, labels, 100,
		//		new KikivenSubgradientEngine<Map<Integer,Double>>(0.1, 500, 0.5, new SoftMarginLoss(10), new GaussianKernel(0.001)));
		//OnlineAlgorithm<Map<Integer,Double>> p = new OnlineAlgorithm<Map<Integer,Double>>(instances, labels,10000, new L1StochasticGradientDescentEngine(new CollinsSGDLearningRate(0.5, instances.size()), 1, instances.size(), 1));
		BinaryOnlineAlgorithm<Map<Integer,Double>> p = new BinaryOnlineAlgorithm<Map<Integer,Double>>(instancesTrain, labelsTrain,100, engine);
		if(engine instanceof KernelPerceptronEngine){
			System.out.println("Number of SVs: " + ((KernelPerceptronEngine)engine).numSVs());
		}else if(engine instanceof KernelPegasosEngine){
			System.out.println("Number of SVs: " + ((KernelPegasosEngine)engine).numSVs());
		}else if(engine instanceof KernelL1NormalizationPegasosEngine){
			System.out.println("Number of SVs: " + ((KernelL1NormalizationPegasosEngine)engine).numSVs());
		}else if(engine instanceof KernelElasticPegasosEngine){
			System.out.println("Number of SVs: " + ((KernelElasticPegasosEngine)engine).numSVs());
		}
		
		//Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>> datasetTest = LibSVMStyleDatasetLoader.load("syntheticLinearTesting.txt");
		Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>> datasetTest = LibSVMStyleDatasetLoader.load("syntheticTestingData.txt");
		//Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>> datasetTest = SpambaseLoader.load("spambase-f"+ ((fold+1)%2) +".data");
		//Pair<Map<Integer,DataObject<Map<Integer,Double>>>, Map<Integer,Boolean>> datasetTest = PimaDiabetesLoader.load("pima-indians-diabetes-f"+ ((fold+1)%2) +".data");
		Map<Integer,DataObject<Map<Integer,Double>>> instancesTest = datasetTest.first();
		Map<Integer,Boolean> labelsTest = datasetTest.second();
		System.out.println(AccuracyComputation.computeAccuracy(instancesTest, labelsTest, p));
	}

	public Map<Long, Double> getWeightVectors() {
		return (Map<Long, Double>) ((LinearPegasosEngine<E>)engine).getWeightVector();
				
	}
}