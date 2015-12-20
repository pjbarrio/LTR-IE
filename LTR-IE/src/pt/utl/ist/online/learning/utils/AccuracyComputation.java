package pt.utl.ist.online.learning.utils;

import java.util.Map;
import java.util.Map.Entry;

import pt.utl.ist.online.learning.BinaryOnlineAlgorithm;
import pt.utl.ist.online.learning.engines.oneclass.OneClassOnlineAlgorithm;
import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;


public class AccuracyComputation {
	public static <E> double computeAccuracy(Map<Integer,DataObject<E>> instances,
			Map<Integer,Boolean> labels, BinaryOnlineAlgorithm<E> p) throws InvalidVectorIndexException{
		double numCorrect=0;
		double numTotal=0;
		for(Entry<Integer,DataObject<E>> entry : instances.entrySet()){
			DataObject<E> vector = entry.getValue();
			Boolean desiredLabel = labels.get(entry.getKey());
			Boolean obtainedLabel = p.classify(vector);
			if(desiredLabel==obtainedLabel){
				numCorrect++;
			}
			numTotal++;
		}
		return numCorrect/numTotal;
	}
	
	public static <E> double computeAccuracy(Map<Integer,DataObject<E>> instances,
			Map<Integer,Boolean> labels, OneClassOnlineAlgorithm<E> p) throws InvalidVectorIndexException{
		double numCorrect=0;
		double numTotal=0;
		for(Entry<Integer,DataObject<E>> entry : instances.entrySet()){
			DataObject<E> vector = entry.getValue();
			Boolean desiredLabel = labels.get(entry.getKey());
			Boolean obtainedLabel = p.classify(vector);
			if(desiredLabel==obtainedLabel){
				numCorrect++;
			}
			numTotal++;
		}
		return numCorrect/numTotal;
	}

}
