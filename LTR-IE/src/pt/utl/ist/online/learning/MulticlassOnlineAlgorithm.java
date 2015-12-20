package pt.utl.ist.online.learning;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pt.utl.ist.online.learning.engines.OnlineEngine;
import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.DataObject;


public class MulticlassOnlineAlgorithm<E,L> implements Serializable {
	
	private OnlineEngine<E> engine;
	
	public MulticlassOnlineAlgorithm(Map<Integer,DataObject<E>> trainingData, Map<Integer,L> labels, int numEpochs, OnlineEngine<E> engine) throws InvalidVectorIndexException{		
		this.engine = engine;
		/*trainingData=convertTrainingData(trainingData);
		
		engine.start();
		for(int currentEpoch=1; currentEpoch<=numEpochs; currentEpoch++){
			int updatesCount=0;
			int elem=0;
			List<Entry<Integer,DataObject<E>>> data = new ArrayList<Entry<Integer,DataObject<E>>>(trainingData.entrySet());
			Collections.shuffle(data);
			for(Entry<Integer,DataObject<E>> entry : data){
				DataObject<E> inputVector = entry.getValue();
				L desiredOutput = labels.get(entry.getKey());
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
		
		engine.compileModel();*/
	}

	public boolean classify(DataObject<E> vector) throws InvalidVectorIndexException{
		vector = engine.convertVector(vector);
		return engine.getResult(vector);
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
}
