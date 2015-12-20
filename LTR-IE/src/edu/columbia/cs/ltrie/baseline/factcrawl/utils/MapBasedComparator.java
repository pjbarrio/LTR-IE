package edu.columbia.cs.ltrie.baseline.factcrawl.utils;

import java.util.Comparator;
import java.util.Map;

public class MapBasedComparator<T> implements Comparator<T> {

	private Map<T, Double> map;
	private boolean descending;

	public MapBasedComparator(Map<T,Double> map){
		this(map,false);
	}
	
	
	
	public MapBasedComparator(Map<T, Double> map, boolean descending) {
		this.map = map;
		this.descending = descending;
	}



	@Override
	public int compare(T o1, T o2) {
		if (!descending)
			return Double.compare(map.get(o2), map.get(o1));
		return Double.compare(map.get(o1), map.get(o2));
	}

}
