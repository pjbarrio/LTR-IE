package pt.utl.ist.online.learning.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class LRUCache<K,V> extends LinkedHashMap<K,V> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final int capacity;

	public LRUCache(int capacity) {
		super(capacity + 1, 1.1f, true);
		this.capacity = capacity;
	}

	protected boolean removeEldestEntry(Entry<K,V> eldest) {
		return size() > capacity;
	}

}
