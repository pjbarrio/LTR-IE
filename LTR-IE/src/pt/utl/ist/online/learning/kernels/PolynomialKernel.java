package pt.utl.ist.online.learning.kernels;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.DataObject;


public class PolynomialKernel extends Kernel<Map<Integer, Double>> {
	
	private final static Integer CONSTANT_DIMENSION = -1;
	double gamma;
	double degree;
	double coef;
	
	public PolynomialKernel(int degree){
		this(degree, 1, 0);
	}
	
	public PolynomialKernel(int degree, double gamma, double coef){
		this.gamma=gamma;
		this.coef=coef;
		this.degree=degree;
	}

	@Override
	protected double computeValue(DataObject<Map<Integer, Double>> x1, DataObject<Map<Integer, Double>> x2) {
		double inner = innerProduct(x1.getData(), x2.getData());
		
		return Math.pow(coef+(gamma*inner), degree);
	}
	
	private double squaredNormOfDiff(Map<Integer,Double> x1,Map<Integer,Double> x2){		
		return squaredNorm(x1)+squaredNorm(x2)-2*innerProduct(x1,x2);
	}
	
	private double squaredNorm(Map<Integer,Double> x){
		double sum = 0.0;
		
		for(Entry<Integer,Double> entry : x.entrySet()){
			sum+=Math.pow(entry.getValue(),2.0);
		}
		
		return sum;
	}
	
	private double innerProduct(Map<Integer,Double> x1,Map<Integer,Double> x2){
		int x1Size = x1.size();
		int x2Size = x2.size();
		if(x2Size<x1Size){
			Map<Integer,Double> temp=x1;
			x1=x2;
			x2=temp;
		}
		
		double result = 0;
		for(Entry<Integer,Double> entryX1 : x1.entrySet()){
			Double valX2 = x2.get(entryX1.getKey());
			if(valX2!=null){
				result+=entryX1.getValue()*valX2;
			}
		}
		return result;
	}
	
	public DataObject<Map<Integer, Double>> convertVector(DataObject<Map<Integer, Double>> x) throws InvalidVectorIndexException{
		if(x.getData().containsKey(CONSTANT_DIMENSION)){
			throw new InvalidVectorIndexException(CONSTANT_DIMENSION);
		}
		
		Map<Integer,Double> result = new HashMap<Integer, Double>(x.getData());
		result.put(CONSTANT_DIMENSION, 1.0);
		return new DataObject<Map<Integer,Double>>(result,x.getId());
	}

}
