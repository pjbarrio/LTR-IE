package pt.utl.ist.online.learning.engines;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.kernels.Kernel;
import pt.utl.ist.online.learning.utils.DataObject;


public class KernelPerceptronEngine<E> implements OnlineEngine<E>, Serializable {
	
	private Map<DataObject<E>,Double> weights;
	private double b=0.0;
	private double learningRate;
	private Kernel<E> kernel;

	public KernelPerceptronEngine(double learningRate, Kernel<E> k) {
		this.learningRate=learningRate;
		weights = new HashMap<DataObject<E>, Double>();
		this.kernel=k;
	}

	public boolean updateModel(DataObject<E> inputVector, boolean desiredOutput){
		boolean currentResult = getResult(inputVector);
		
		if(desiredOutput && !currentResult){
			addSV(inputVector,learningRate);
			b-=learningRate;
			return true;
		}else if(!desiredOutput && currentResult){
			addSV(inputVector,-learningRate);
			b+=learningRate;
			return true;
		}
		
		return false;
	}
	
	public boolean getResult(DataObject<E> vector){
		int currentResult = (int) Math.signum(decisionFunction(vector)-b);
		if(currentResult>0){
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public double getScore(DataObject<E> vector) {
		return decisionFunction(vector)-b;
	}
	
	private double decisionFunction(DataObject<E> x){
		double sum = 0.0;
		for(Entry<DataObject<E>,Double> supportVector : weights.entrySet()){
			DataObject<E> sv = supportVector.getKey();
			double weight = supportVector.getValue();
			
			sum+= weight*kernel.compute(sv, x);
		}
		return sum;
	}
	
	private void addSV(DataObject<E> x, double learningRate){
		Double currentWeight = weights.get(x);
		if(currentWeight==null){
			currentWeight=0.0;
		}
		currentWeight+=learningRate;
		if(currentWeight!=0){
			weights.put(x, currentWeight);
		}else{
			weights.remove(x);
		}
	}
	
	public DataObject<E> convertVector(DataObject<E> x) throws InvalidVectorIndexException{
		return x;
	}

	public int numSVs() {
		return weights.size();
	}
	
	@Override
	public void compileModel() {
		kernel.clear();
		kernel.setUseCache(false);
	}

	@Override
	public void start() {
		kernel.setUseCache(true);
	}

	@Override
	public void objectiveReport(Map<Integer, DataObject<E>> trainingData, Map<Integer, Boolean> desiredLabels) {
		// TODO Auto-generated method stub
		
	}

}
