import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class EfficientDoubleMap<K> implements Map<K, Double> {

	private TObjectDoubleHashMap<K> map = new TObjectDoubleHashMap<K>();
	
	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		if(value instanceof Double){
			return map.containsValue((Double) value);
		}
		return false;
	}

	@Override
	public Set<Entry<K, Double>> entrySet() {
		HashSet<Entry<K,Double>> result = new HashSet<Entry<K,Double>>();
		for(K key : map.keySet()){
			result.add(new MyEntry(key, map.get(key)));
		}
		return result;
	}

	@Override
	public Double get(Object key) {
		return map.get(key);
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Double put(K key, Double value) {
		return map.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends Double> m) {
		map.putAll(m);
	}

	@Override
	public Double remove(Object key) {
		return map.remove(key);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Collection<Double> values() {
		List<Double> values = new ArrayList<Double>();
		
		for(double d : map.values()){
			values.add(d);
		}
		return values;
	}
	
	private class MyEntry implements Entry<K,Double>{
		
		private K key;
		private Double v;
		
		private MyEntry(K key, Double v){
			this.key=key;
			this.v=v;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public Double getValue() {
			return v;
		}

		@Override
		public Double setValue(Double value) {
			throw new UnsupportedOperationException();
		}
		
	}

}
