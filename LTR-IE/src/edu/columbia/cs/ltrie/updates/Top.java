package edu.columbia.cs.ltrie.updates;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

import edu.columbia.cs.ltrie.baseline.factcrawl.utils.MapBasedComparator;

public class Top<T> implements Iterable<T> {

	private int capacity;
	private PriorityQueue<T> list;
	private Comparator<T> comp;
	public Top(int capacity, Map<T,Double> map) {
		this.capacity = capacity;
		
		comp = new MapBasedComparator<T>(map,true);
		
		list = new PriorityQueue<T>(capacity,comp);
	}

	@Override
	public Iterator<T> iterator() {
		return list.iterator();
	}

	public boolean put(T key) {
		if (list.size() < capacity){
			return list.add(key);
		}else{
			
			T el = list.element();
			
			//System.err.println(el);
			
			if (comp.compare(key, el) > 0){
				list.remove();
				list.add(key);
				return true;
			}else{
				return false;
			}
			
		}
	}

	public static void main(String[] args) {
		
		Map<Long,Double> map = new HashMap<Long, Double>();
		map.put(1L, 1d);
		map.put(2L, 2d);
		map.put(3L, 3d);
		map.put(4L, 4d);
		map.put(5L, 5d);
				
		Top<Long> e = new Top<Long>(3,map);
		
		e.put(1L);
		e.put(2L);
		e.put(3L);
		e.put(4L);
		e.put(5L);
		
		for (Long long1 : e) {
			System.out.println(long1);
		}
		
	}
	
}
