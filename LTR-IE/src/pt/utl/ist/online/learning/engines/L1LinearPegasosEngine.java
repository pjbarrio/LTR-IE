package pt.utl.ist.online.learning.engines;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.DataObject;


public class L1LinearPegasosEngine<E> implements LinearOnlineEngine<E> {

	private Map<E,Double> weights;
	private Map<E,Double> qi;
	private double u;
	private double b=0.0;
	private double hingeParameter;
	private double lambda;
	private double t=0;
	private double normSq=0.0;
	private boolean useB;
	
	private L1LinearPegasosEngine(){
		
	}

	public L1LinearPegasosEngine(double lambda, double hingeParameter) {
		this(lambda,hingeParameter,true);
	}

	public L1LinearPegasosEngine(double lambda, double hingeParameter, boolean useB) {
		this.lambda=lambda;
		this.hingeParameter=hingeParameter;
		weights = new HashMap<E, Double>();
		qi = new HashMap<E, Double>();
		u = 0;
		this.useB=useB;
	}

	public boolean updateModel(DataObject<Map<E, Double>> inputVector, boolean desiredOutput){
		t++;
		double y=1.0;
		if(!desiredOutput){
			y=-1.0;
		}
		double inner = innerProduct(weights, inputVector.getData());

		double learning = 1.0/(lambda*t);
		u+=(1.0/t);
		updateWeights(inputVector,learning,y,inner);
		double norm= Math.sqrt(normSq);
		double constant = Math.min(1.0, 1.0/(norm*Math.sqrt(lambda)));
		if(constant!=1.0){
			for(Entry<E,Double> entryX1 : weights.entrySet()){
				double original = entryX1.getValue();
				double newVal = original*constant;
				entryX1.setValue(newVal);
			}
			double before = normSq;
			normSq*=constant*constant;
		}
		
		return true;
	}

	private void updateWeights(DataObject<Map<E, Double>> inputVector, double learning, double y, double inner) {
		double inputVectorConstant= learning*y;
		boolean toUseInputVector = y*(inner-b)<=hingeParameter;
		for(Entry<E,Double> entry : inputVector.getData().entrySet()){
			double xVal = entry.getValue();
			Double newWi = weights.get(entry.getKey());
			boolean isNewValue=false;
			if(newWi==null){
				newWi=0.0;
				isNewValue=true;
			}
			double originalValue=newWi;
			if(toUseInputVector){
				newWi+=inputVectorConstant*xVal;
			}
			applyPenalty(entry.getKey(),newWi,isNewValue,originalValue);
		}
		if(toUseInputVector && useB){
			b-=y/(t*lambda);
		}
	}

	private void applyPenalty(E key, double newWi,boolean isNewValue, double originalValue) {
		double z = newWi;
		Double currentQ = qi.get(key);
		if(currentQ==null){
			currentQ=0.0;
		}
		if(newWi>0){
			newWi=Math.max(0, newWi-(u+currentQ));
		}else if(newWi<0){
			newWi=Math.min(0, newWi+(u-currentQ));
		}
		qi.put(key, currentQ+(newWi-z));
		if(newWi!=0){
			weights.put(key, newWi);
		}else if(!isNewValue){
			weights.remove(key);
		}
		double diff=newWi-originalValue;
		normSq+=2*diff*originalValue+diff*diff;
		if(normSq<0){
			normSq=0;
		}
	}

	public boolean getResult(DataObject<Map<E, Double>> vector){
		double currentResult = innerProduct(weights, vector.getData())-b;
		if(currentResult>0){
			return true;
		}else{
			return false;
		}
	}

	@Override
	public double getScore(DataObject<Map<E, Double>> vector) {
		double inner = innerProduct(weights, vector.getData());
		return inner-b;
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

	public DataObject<Map<E, Double>> convertVector(DataObject<Map<E, Double>> x) throws InvalidVectorIndexException{
		return x;
	}

	@Override
	public void compileModel() {
		double totalSum=0.0;
		int numFeatures=weights.size();
		double maxValue=Double.MIN_VALUE;
		double minValue=Double.MAX_VALUE;
		double absoluteMin=Double.MAX_VALUE;
		for(Entry<E,Double> entry : weights.entrySet()){
			totalSum+=entry.getValue();
			maxValue=Math.max(maxValue, entry.getValue());
			minValue=Math.min(minValue, entry.getValue());
			absoluteMin=Math.min(absoluteMin, Math.abs(entry.getValue()));
		}
		System.out.println("Total Sum = " + totalSum);
		System.out.println("Average = " + totalSum/numFeatures);
		System.out.println("Num Features = " + numFeatures);
		System.out.println("Max value = " + maxValue);
		System.out.println("Min value = " + minValue);
		System.out.println("Absolute min = " + absoluteMin);
		//System.out.println(qi);
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
		L1LinearPegasosEngine<E> newEngine = new L1LinearPegasosEngine<E>();
		newEngine.weights=new HashMap<E, Double>(weights);
		newEngine.qi= new HashMap<E, Double>(qi);
		newEngine.b=b;
		newEngine.hingeParameter=hingeParameter;
		newEngine.lambda=lambda;
		newEngine.t=t;
		newEngine.normSq=normSq;
		newEngine.useB=useB;
		newEngine.u=u;
		
		return newEngine;
	}

}
