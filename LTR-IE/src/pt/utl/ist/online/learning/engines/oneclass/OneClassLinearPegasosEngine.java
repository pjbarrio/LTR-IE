package pt.utl.ist.online.learning.engines.oneclass;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.DataObject;


public class OneClassLinearPegasosEngine<E> implements OneClassEngine<Map<E,Double>> {

	private Map<E,Double> weights;
	private double hingeParameter;
	private double fraction;
	private double t=0;

	public OneClassLinearPegasosEngine(double fraction) {
		this.fraction=fraction;
		weights = new HashMap<E, Double>();
	}
	
	@Override
	public boolean updateModel(DataObject<Map<E, Double>> inputVector) {
		t++;
		double learning = fraction/t;
		
		double inner = innerProduct(weights, inputVector.getData());
		boolean toUseInputVector = hingeParameter>inner;
		double weightVectorconstant = (1-learning/fraction);
		normalizeWeights(weightVectorconstant);
		hingeParameter+=learning;
		if(toUseInputVector){
			double inputVectorConstant= learning/fraction;
			for(Entry<E,Double> xVect : inputVector.getData().entrySet()){
				Double valW = weights.get(xVect.getKey());
				double xVal=xVect.getValue();
				if(valW==null){
					valW=0.0;
				}
				weights.put(xVect.getKey(), valW+inputVectorConstant*xVal);
			}
			hingeParameter-=inputVectorConstant;
		}
		
		double multConstant=Math.min(1.0, Math.sqrt(fraction/normWeightsSq()));
		normalizeWeights(multConstant);
		hingeParameter*=multConstant;
		
		
		return false;
	}

	private double normWeightsSq() {
		double normSq=0;
		
		for(Entry<E,Double> entryX1 : weights.entrySet()){
			double original = entryX1.getValue();
			normSq+=original*original;
		}
		
		return normSq;
	}
	
	private void normalizeWeights(double k){
		for(Entry<E,Double> entryX1 : weights.entrySet()){
			double original = entryX1.getValue();
			double newVal = original*k;
			entryX1.setValue(newVal);
		}
	}

	public boolean getResult(DataObject<Map<E, Double>> vector){
		double currentResult = innerProduct(weights, vector.getData())-hingeParameter;
		if(currentResult>0){
			return true;
		}else{
			return false;
		}
	}

	@Override
	public double getScore(DataObject<Map<E, Double>> vector) {
		double inner = innerProduct(weights, vector.getData());
		return inner-hingeParameter;
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
}
