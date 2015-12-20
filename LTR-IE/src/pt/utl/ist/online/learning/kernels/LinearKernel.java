package pt.utl.ist.online.learning.kernels;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.DataObject;


public class LinearKernel extends Kernel<Map<Integer, Double>> {

	private final static Integer CONSTANT_DIMENSION = -1;
	
	@Override
	protected double computeValue(DataObject<Map<Integer, Double>> v1, DataObject<Map<Integer, Double>> v2) {
		Map<Integer, Double> x1 = v1.getData();
		Map<Integer, Double> x2 = v2.getData();
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
