package edu.columbia.cs.ltrie.baseline.factcrawl.graph.utils;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.brettw.SparseBitSet;

import pt.utl.ist.online.learning.utils.MemoryEfficientHashMap;

public class BitBasedBiMap<T2> {

	Map<Integer,Map<T2,Integer>> m1;
	
	Map<T2,SparseBitSet> m2;

	/*public void add(Integer t1, T2 t2) {
		
		getM1(t1).put(t2,getM1(t1).size());
		getM2(t2).set(t1);
		
	}*/
	
	Map<T2,Integer> m2Sizes = new MemoryEfficientHashMap<T2, Integer>();

	public void add(Integer t1, T2 t2) {
		Integer val = m2Sizes.get(t2);
		if(val==null){
			val=0;
		}
		getM1(t1).put(t2,val);
		getM2(t2).set(t1);
		m2Sizes.put(t2, val+1);
		
	}

	private SparseBitSet getM2(T2 t2) {
		SparseBitSet bitSet = getM2().get(t2);
		if (bitSet == null){
			bitSet = new SparseBitSet();
			getM2().put(t2,bitSet);
		}
		return bitSet;
	}

	private Map<T2, SparseBitSet> getM2() {
		if (m2 == null){
			m2 = new MemoryEfficientHashMap<T2, SparseBitSet>();
		}
		return m2;
	}

	private Map<T2,Integer> getM1(Integer t1) {
		Map<T2,Integer> list = getM1().get(t1);
		if (list == null){
			list = new MemoryEfficientHashMap<T2,Integer>();
			getM1().put(t1,list);
		}
		return list;
	}

	private Map<Integer, Map<T2,Integer>> getM1() {
		if (m1 == null){
			m1 = new MemoryEfficientHashMap<Integer, Map<T2,Integer>>();
		}
		return m1;
	}

	public Set<T2> getByFirst(Integer t1) {
		return getM1(t1).keySet();
	}

	public Set<Integer> getBySecond(T2 t2) {
		SparseBitSet bitSet = getM2(t2);
		if(bitSet==null){
			return new HashSet<Integer>();
		}
		
		Set<Integer> set = new HashSet<Integer>();
		for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i+1)) {
		    set.add(i);
		 }
		
		return set;
	}

	public int getPosition(Integer t1, T2 t2) {
		return getM1(t1).get(t2);
	}
	
}
