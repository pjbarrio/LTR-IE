package pt.utl.ist.online.learning.engines;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.DataObject;


public interface OnlineEngine<E> extends Serializable {
	public boolean updateModel(DataObject<E> inputVector, boolean desiredOutput);
	public boolean getResult(DataObject<E> vector);
	public DataObject<E> convertVector(DataObject<E> x) throws InvalidVectorIndexException;
	public void compileModel();
	public void objectiveReport(Map<Integer, DataObject<E>> trainingData, Map<Integer, Boolean> desiredLabels);
	public void start();
	public double getScore(DataObject<E> vector);
}
