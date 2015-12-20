package pt.utl.ist.online.learning.engines;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.DataObject;


public class LinearPerceptronEngine<E> implements LinearOnlineEngine<E> {
	
	private Map<E,Double> weights;
	private double b=0.0;
	private double learningRate;
	
	private LinearPerceptronEngine(){
		
	}

	public LinearPerceptronEngine(double learningRate) {
		this.learningRate=learningRate;
		weights = new HashMap<E, Double>();
	}

	public boolean updateModel(DataObject<Map<E, Double>> inputVector, boolean desiredOutput){
		boolean currentResult = getResult(inputVector);
		
		if(desiredOutput && !currentResult){
			weights=sumVectors(weights,inputVector.getData(),learningRate);
			b-=learningRate;
			return true;
		}else if(!desiredOutput && currentResult){
			weights=sumVectors(weights,inputVector.getData(),-learningRate);
			b+=learningRate;
			return true;
		}
				
		return false;
	}
	
	public boolean getResult(DataObject<Map<E, Double>> vector){
		int currentResult = (int) Math.signum(innerProduct(weights, vector.getData())-b);
		if(currentResult>0){
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public double getScore(DataObject<Map<E, Double>> vector) {
		return innerProduct(weights, vector.getData())-b;
	}
	
	private double innerProduct(Map<E,Double> x1,Map<E,Double> x2){
		int x1Size = x1.size();
		int x2Size = x2.size();
		if(x2Size<x1Size){
			Map<E,Double> temp=x1;
			x1=x2;
			x2=temp;
		}
		
		double result = 0;
		for(Entry<E,Double> entryX1 : x1.entrySet()){
			Double valX2 = x2.get(entryX1.getKey());
			if(valX2!=null){
				result+=entryX1.getValue()*valX2;
			}
		}
		return result;
	}
	
	private Map<E,Double> sumVectors(Map<E,Double> x1,Map<E,Double> x2, double learningRate){
		Map<E,Double> result = new HashMap<E, Double>(x2);
		for(Entry<E,Double> entryX1 : x1.entrySet()){
			Double valX2 = result.get(entryX1.getKey());
			if(valX2==null){
				result.put(entryX1.getKey(), entryX1.getValue());
			}else{
				result.put(entryX1.getKey(), entryX1.getValue()+valX2*learningRate);
			}
		}
		return result;
	}
	
	public DataObject<Map<E, Double>> convertVector(DataObject<Map<E, Double>> x) throws InvalidVectorIndexException{
		/*if(x.getData().containsKey(CONSTANT_DIMENSION)){
			throw new InvalidVectorIndexException(CONSTANT_DIMENSION);
		}
		
		Map<Long,Double> result = new HashMap<Long, Double>(x.getData());
		result.put(CONSTANT_DIMENSION, 1.0);
		return new DataObject<Map<Long,Double>>(result,x.getId());*/
		return x;
	}
	
	@Override
	public void compileModel() {
	}

	@Override
	public void start() {
	}

	@Override
	public void objectiveReport(Map<Integer, DataObject<Map<E, Double>>> trainingData, Map<Integer, Boolean> desiredLabels){
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public Map<E, Double> getWeightVector() {
		return weights;
	}
	
	@Override
	public double getRho() {
		return b;
	}

	@Override
	public LinearOnlineEngine<E> copy() {
		LinearPerceptronEngine<E> newEngine = new LinearPerceptronEngine<E>();
		newEngine.weights=new HashMap<E, Double>(weights);
		newEngine.b=b;
		newEngine.learningRate=learningRate;
		return newEngine;
	}

}
