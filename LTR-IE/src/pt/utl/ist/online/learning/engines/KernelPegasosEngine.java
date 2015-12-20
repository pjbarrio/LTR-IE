package pt.utl.ist.online.learning.engines;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.kernels.Kernel;
import pt.utl.ist.online.learning.utils.DataObject;


public class KernelPegasosEngine<E> implements OnlineEngine<E>, Serializable {
	
	private Map<DataObject<E>,Double> weights;
	private double b=0.0;
	private double currentNormSquared=0;
	private double lambda;
	private double hingeParameter;
	private Kernel<E> kernel;
	private double t=0;
	private int updates=0;
	private int updatesP=0;
	private int updatesN=0;

	public KernelPegasosEngine(double lambda, double hingeParameter, Kernel<E> k) {
		this.lambda=lambda;
		weights = new HashMap<DataObject<E>, Double>();
		this.kernel=k;
		this.hingeParameter=hingeParameter;
	}

	public boolean updateModel(DataObject<E> inputVector, boolean desiredOutput){
		t++;
		double y=1.0;
		if(!desiredOutput){
			y=-1.0;
		}
		double inner = decisionFunction(inputVector);
		
		reWeight(1.0-(1/t));
				
		if(y*(inner-b)<=hingeParameter){
			//System.out.println(desiredOutput + " " + inner + " " + b + " " + (inner-b));
			updates++;
			if(desiredOutput){
				updatesP++;
			}else{
				updatesN++;
			}
			Double alpha = weights.get(inputVector);
			if(alpha==null){
				alpha=0.0;
			}
			
			double diff=y/(t*lambda); 
			alpha+=diff;
			b-=diff;
			
			currentNormSquared+=((2*diff*inner*(1.0-(1/t)))+Math.pow(diff, 2)*kernel.compute(inputVector, inputVector));
			weights.put(inputVector, alpha);
		}
		
		double norm=Math.sqrt(currentNormSquared);
		reWeight(Math.min(1.0, 1.0/(norm*Math.sqrt(lambda))));
		return true;
	}

	private void reWeight(double d) {
		currentNormSquared*=(d*d);
		for(Entry<DataObject<E>,Double> supportVector : weights.entrySet()){
			DataObject<E> sv = supportVector.getKey();
			weights.put(sv,supportVector.getValue()*d);
		}
	}

	public boolean getResult(DataObject<E> vector){
		double currentResult = decisionFunction(vector)-b;
		kernel.clear();
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
	
	public DataObject<E> convertVector(DataObject<E> x) throws InvalidVectorIndexException{
		return x;
	}

	public int numSVs() {
		System.out.println(updates + " Updates");
		System.out.println(updatesP + " Positive Updates");
		System.out.println(updatesN + " Negative Updates");
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
