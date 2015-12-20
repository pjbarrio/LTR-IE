package pt.utl.ist.online.learning.utils;

import java.io.Serializable;
import java.util.Map;

public abstract class Statistics implements Serializable {
	
	private static final long serialVersionUID = -1580650979298240776L;
	
	private Map<String, Integer> results;
	
	public Statistics(Map<String, Integer> results) {
		this.results = results;
	}
	
	public Map<String, Integer> getResults() {
		return results;
	}
}
