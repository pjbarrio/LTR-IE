package edu.columbia.cs.ltrie.baseline.factcrawl.graph.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.Query;

import pt.utl.ist.online.learning.utils.MemoryEfficientHashMap;

public class BiMap<T1, T2> {

	Map<T1,Map<T2,Integer>> m1;
	
	Map<T2,Set<T1>> m2;

	public void add(T1 t1, T2 t2) {
		
		getM1(t1).put(t2,getM1(t1).size());
		getM2(t2).add(t1);
		
	}

	private Set<T1> getM2(T2 t2) {
		Set<T1> list = getM2().get(t2);
		if (list == null){
			list = new HashSet<T1>();
			getM2().put(t2,list);
		}
		return list;
	}

	private Map<T2, Set<T1>> getM2() {
		if (m2 == null){
			m2 = new MemoryEfficientHashMap<T2, Set<T1>>();
		}
		return m2;
	}

	private Map<T2,Integer> getM1(T1 t1) {
		Map<T2,Integer> list = getM1().get(t1);
		if (list == null){
			list = new MemoryEfficientHashMap<T2,Integer>();
			getM1().put(t1,list);
		}
		return list;
	}

	private Map<T1, Map<T2,Integer>> getM1() {
		if (m1 == null){
			m1 = new MemoryEfficientHashMap<T1, Map<T2,Integer>>();
		}
		return m1;
	}

	public Set<T2> getByFirst(T1 t1) {
		return getM1(t1).keySet();
	}

	public Set<T1> getBySecond(T2 t2) {
		return getM2(t2);
	}

	public int getPosition(T1 t1, T2 t2) {
		return getM1(t1).get(t2);
	}
	
}
