package pt.utl.ist.online.learning.engines.oneclass;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.kernels.Kernel;
import pt.utl.ist.online.learning.utils.DataObject;
import pt.utl.ist.online.learning.utils.Pair;


public class OneClassKernelPegasosEngine<E> implements OneClassEngine<E> {
	
	private Map<DataObject<E>,Double> weights;
	private double currentNormSquared=0;
	private double fraction;
	private double hingeParameter;
	private Kernel<E> kernel;
	private double t=0;
	private int updates=0;
	private int updatesP=0;
	private int updatesN=0;

	public OneClassKernelPegasosEngine(double fraction, Kernel<E> k) {
		this.fraction=fraction;
		weights = new HashMap<DataObject<E>, Double>();
		this.kernel=k;
	}
	
	@Override
	public boolean updateModel(DataObject<E> inputVector) {
		t++;
		double learning = fraction/t;
		
		double inner = decisionFunction(inputVector);
		boolean toUseInputVector = hingeParameter>inner;
		double weightVectorconstant = (1-learning/fraction);
		reWeight(weightVectorconstant);
		hingeParameter+=learning;
		
		if(toUseInputVector){
			Double alpha = weights.get(inputVector);
			if(alpha==null){
				alpha=0.0;
			}
			
			double diff=learning/fraction; 
			alpha+=diff;
			hingeParameter-=diff;
			
			currentNormSquared+=((2*diff*inner*(1.0-(1/t)))+Math.pow(diff, 2)*kernel.compute(inputVector, inputVector));
			weights.put(inputVector, alpha);
		}
		
		double multConstant=Math.min(1.0, Math.sqrt(fraction/currentNormSquared));
		reWeight(multConstant);
		hingeParameter*=multConstant;
		
		if(t%1000==0){
			System.out.println(weights.size() + " Support Vectors.");
		}
		return false;
	}

	private void reWeight(double d) {
		currentNormSquared*=(d*d);
		for(Entry<DataObject<E>,Double> supportVector : weights.entrySet()){
			supportVector.setValue(supportVector.getValue()*d);
		}
	}

	public boolean getResult(DataObject<E> vector){
		double currentResult = decisionFunction(vector)-hingeParameter;
		kernel.clear();
		if(currentResult>0){
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public double getScore(DataObject<E> vector) {
		return decisionFunction(vector)-hingeParameter;
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
		return weights.size();
	}
	
	@Override
	public void compileModel() {
		kernel.clear();
		kernel.setUseCache(false);
		System.out.println(weights.size() + " support vectors for Feature Shifting.");
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
