package pt.utl.ist.online.learning.kernels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.DataObject;
import pt.utl.ist.online.learning.utils.LRUCache;
import pt.utl.ist.online.learning.utils.Pair;


public class GaussianKernel extends Kernel<Map<Long, Double>> {

	private final static Long CONSTANT_DIMENSION = -1L;
	double gamma;
	private static LRUCache<Integer,Double> norms = new LRUCache<Integer,Double>(5000);
	private static LRUCache<Integer,List<VectorEntry>> vectors = new LRUCache<Integer,List<VectorEntry>>(5000);

	public GaussianKernel(double gamma){
		this.gamma=gamma;
	}

	@Override
	protected double computeValue(DataObject<Map<Long, Double>> x1, DataObject<Map<Long, Double>> x2) {
		double exp = -gamma*squaredNormOfDiff(x1, x2);
		return Math.exp(exp);
	}

	private double squaredNormOfDiff(DataObject<Map<Long, Double>> x1,DataObject<Map<Long, Double>> x2){	
		/*Set<Long> keys = new HashSet<Long>();
		keys.addAll(x1.keySet());
		keys.addAll(x2.keySet());

		double norm1=0;
		double norm2=0;
		double inner=0;
		for(Long key : keys){
			Double w1 = x1.get(key);
			Double w2 = x2.get(key);

			if(w1!=null){
				double w = w1;
				norm1+=w*w;
			}
			if(w2!=null){
				double w = w2;
				norm2+=w*w;
			}
			if(w1!=null && w2!=null){
				inner+=w1*w2;
			}
		}
		return norm1+norm2-2*inner;*/
		return squaredNorm(x1)+squaredNorm(x2)-2*innerProduct2(getSortedVector(x1),getSortedVector(x2));
	}

	private double innerProduct2(List<VectorEntry> x,
			List<VectorEntry> y) {
		double sum = 0;
		int xlen = x.size();
		int ylen = y.size();
		if(xlen!=0 && ylen!=0){
			int i = 0;
			VectorEntry xCur = x.get(i);
			int j = 0;
			VectorEntry yCur = y.get(j);
			while(i < xlen && j < ylen)
			{
				if(xCur.key == yCur.key){
					sum += xCur.value * yCur.value;
					++i;
					if(i < xlen){
						xCur = x.get(i);
					}
					++j;
					if(j < ylen){
						yCur = y.get(j);
					}
				}else {
					if(xCur.key > yCur.key){
						++j;
						if(j < ylen){
							yCur = y.get(j);
						}
					}else{
						++i;
						if(i < xlen){
							xCur = x.get(i);
						}
					}
				}
			}
		}
		return sum;
	}

	private List<VectorEntry> getSortedVector(DataObject<Map<Long, Double>> x){
		List<VectorEntry> sortedVector = vectors.get(x.getId());
		if(sortedVector==null){
			sortedVector = new ArrayList<VectorEntry>();
			for(Entry<Long,Double> entry: x.getData().entrySet()){
				sortedVector.add(new VectorEntry(entry.getKey(), entry.getValue()));
			}
			Collections.sort(sortedVector, new Comparator<VectorEntry>() {
				@Override
				public int compare(VectorEntry o1, VectorEntry o2) {
					return (int) Math.signum(o1.key-o2.key);
				}
			});

			vectors.put(x.getId(), sortedVector);
		}
		return sortedVector;
	}

	private double squaredNorm(DataObject<Map<Long, Double>> x){
		Double norm = norms.get(x.getId());
		if(norm==null){
			double sum = 0.0;


			for(Entry<Long,Double> entry : x.getData().entrySet()){
				sum+=Math.pow(entry.getValue(),2.0);
			}

			norms.put(x.getId(), sum);
			return sum;
		}
		return norm;
	}

	private double innerProduct(Map<Long,Double> x1,Map<Long,Double> x2){
		int x1Size = x1.size();
		int x2Size = x2.size();
		if(x2Size<x1Size){
			Map<Long,Double> temp=x1;
			x1=x2;
			x2=temp;
		}

		double result = 0;
		for(Entry<Long,Double> entryX1 : x1.entrySet()){
			Double valX2 = x2.get(entryX1.getKey());
			if(valX2!=null){
				result+=entryX1.getValue()*valX2;
			}
		}
		return result;
	}

	public DataObject<Map<Long, Double>> convertVector(DataObject<Map<Long, Double>> x) throws InvalidVectorIndexException{
		if(x.getData().containsKey(CONSTANT_DIMENSION)){
			throw new InvalidVectorIndexException(CONSTANT_DIMENSION);
		}

		Map<Long,Double> result = new HashMap<Long, Double>(x.getData());
		result.put(CONSTANT_DIMENSION, 1.0);
		return new DataObject<Map<Long,Double>>(result,x.getId());
	}
	
	private class VectorEntry{
		long key;
		double value;
		
		public VectorEntry(long key, double value){
			this.key=key;
			this.value=value;
		}
	}

}
