package pt.utl.ist.online.learning.engines;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.DataObject;


public class LinearPegasosEngine<E> implements LinearOnlineEngine<E> {

	private Map<E,Double> weights;
	private double b=0.0;
	private double hingeParameter;
	private double lambda;
	private double t=0;
	private double normSq=0.0;
	private boolean useB;
	private double previousMult=1.0;
	
	private LinearPegasosEngine(){
		
	}

	public LinearPegasosEngine(double lambda, double hingeParameter) {
		this(lambda,hingeParameter,true);
	}

	public LinearPegasosEngine(double lambda, double hingeParameter, boolean useB) {
		this.lambda=lambda;
		this.hingeParameter=hingeParameter;
		weights = new HashMap<E, Double>();
		this.useB=useB;
	}

	public boolean updateModel(DataObject<Map<E, Double>> inputVector, boolean desiredOutput){
		t++;
		double y=1.0;
		if(!desiredOutput){
			y=-1.0;
		}
		double inner = innerProduct(weights, inputVector.getData())*previousMult;

		double learning = 1.0/(lambda*t);
		boolean toUseInputVector = y*(inner-b)<=hingeParameter;
		double weightVectorconstant = (1-learning*lambda);
		for(Entry<E,Double> entryX1 : weights.entrySet()){
			double original = entryX1.getValue();
			double newVal = original*weightVectorconstant*previousMult;
			entryX1.setValue(newVal);
		}
		normSq*=weightVectorconstant*weightVectorconstant;
		if(toUseInputVector){
			double inputVectorConstant= learning*y;
			double normVectorSq=0.0;
			for(Entry<E,Double> xVect : inputVector.getData().entrySet()){
				Double valW = weights.get(xVect.getKey());
				double xVal=xVect.getValue();
				if(valW==null){
					valW=0.0;
				}
				normVectorSq+=xVal*xVal;
				weights.put(xVect.getKey(), valW+inputVectorConstant*xVal);
			}
			if(useB){
				b-=y/(t*lambda);
			}
			normSq+=(inputVectorConstant*inputVectorConstant)*normVectorSq+2*inputVectorConstant*inner*weightVectorconstant;
		}
		
		double multConstant=Math.min(1.0, 1.0/(Math.sqrt(normSq)*Math.sqrt(lambda)));
		//multConstant(weights, multConstant);
		previousMult=multConstant;
		normSq*=multConstant*multConstant;
		b*=multConstant;

		return true;
	}

	public boolean getResult(DataObject<Map<E, Double>> vector){
		double currentResult = innerProduct(weights, vector.getData())-b;
		System.out.println(currentResult);
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
		LinearPegasosEngine<E> newEngine = new LinearPegasosEngine<E>();
		newEngine.weights=new HashMap<E, Double>(weights);
		newEngine.b=b;
		newEngine.hingeParameter=hingeParameter;
		newEngine.lambda=lambda;
		newEngine.t=t;
		newEngine.normSq=normSq;
		newEngine.useB=useB;
		newEngine.previousMult=previousMult;
		
		return newEngine;
	}

}
