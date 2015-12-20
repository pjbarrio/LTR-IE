package edu.columbia.cs.ltrie.datamodel;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Tuple implements Serializable {
	private Map<String,Span> tupleData = new HashMap<String,Span>();
	
	public Span getData(String attributeKey){
		return tupleData.get(attributeKey);
	}
	
	public void setData(String attributeKey, Span data){
		tupleData.put(attributeKey, data);
	}
	
	public String toString(){
		return tupleData.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Tuple){
			return tupleData.equals(((Tuple) o).tupleData);
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return tupleData.hashCode();
	}

	public Set<String> getFieldNames() {
		return tupleData.keySet();
	}
	
	public int getSize(){
		return tupleData.size();
	}
}
