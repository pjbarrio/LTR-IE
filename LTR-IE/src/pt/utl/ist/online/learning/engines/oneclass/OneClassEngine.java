package pt.utl.ist.online.learning.engines.oneclass;

import java.util.Map;

import pt.utl.ist.online.learning.utils.DataObject;

public interface OneClassEngine<E> {
	public boolean updateModel(DataObject<E> inputVector);
	public boolean getResult(DataObject<E> vector);
	public void compileModel();
	public void objectiveReport(Map<Integer, DataObject<E>> trainingData, Map<Integer, Boolean> desiredLabels);
	public void start();
	public double getScore(DataObject<E> vector);
}
