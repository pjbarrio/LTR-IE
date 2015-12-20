package pt.utl.ist.online.learning.kernels;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pt.utl.ist.online.learning.BinaryOnlineAlgorithm;
import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.DataObject;
import pt.utl.ist.online.learning.utils.Pair;
import pt.utl.ist.online.learning.utils.UnorderedPair;


public abstract class Kernel<E> implements Serializable{
	
	private boolean useCache = false;
	private int reused;
	private HashMap<Integer,HashMap<Integer,Double>> kernelCache = new HashMap<Integer,HashMap<Integer,Double>>();
	
	public double compute(DataObject<E> x1, DataObject<E> x2){
		if(useCache){
			return computeWithCache(x1, x2);
		}else{
			return computeWithoutCache(x1, x2);
		}
	}
	
	private double computeWithCache(DataObject<E> x1, DataObject<E> x2) {
		int id1 = x1.getId();
		int id2 = x2.getId();
		if(id1>id2){
			int temp = id1;
			id1=id2;
			id2=temp;
			
			DataObject<E> tempV = x1;
			x1=x2;
			x2=tempV;
		}
		
		HashMap<Integer, Double> innerMap = kernelCache.get(id1);
		if(innerMap==null){
			innerMap=new HashMap<Integer, Double>();
			kernelCache.put(id1, innerMap);
		}
		
		Double result = innerMap.get(id2);
		if(result==null){
			result = computeValue(x1, x2);
			innerMap.put(id2, result);
		}
		
		return result;
	}
	
	private double computeWithoutCache(DataObject<E> x1, DataObject<E> x2){
		return computeValue(x1, x2);
	}
	
	protected abstract double computeValue(DataObject<E> v1, DataObject<E> v2);
	public abstract DataObject<E> convertVector(DataObject<E> x) throws InvalidVectorIndexException;

	public void setUseCache(boolean useCache) {
		this.useCache=useCache;
	}
	
	public void clear() {
		kernelCache.clear();
	}
}
