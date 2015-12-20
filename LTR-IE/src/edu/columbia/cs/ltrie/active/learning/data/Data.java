package edu.columbia.cs.ltrie.active.learning.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.columbia.cs.ltrie.utils.SerializationHelper;

import weka.core.Instances;

public abstract class Data<T,I> {

	T data;
	List<String> suffixes;
	
	private String fileName;

	public Data(T data) {
		this.data = data;
		suffixes = new ArrayList<String>();
	}

	public Data(String instances) throws Exception {
		data = loadFromString(instances);
		suffixes = new ArrayList<String>();
		fileName = instances;


	}

	protected T loadFromString(String instances) throws Exception{
		return (T)SerializationHelper.read(instances);
	}

	public T getInstances() {
		return data;
	}

	protected void setInstances(T newData) {
		data = newData;
	}
	
	public abstract Data<T,I> select(int numOfFeatures) throws Exception;

	public void informProcessing(String suffix){
		suffixes.add(suffix);
	}
	
	public void saveData() throws IOException{
		saveData(fileName);
	}
	
	protected String addSuffixes(String fileName) {
		
		int i = fileName.lastIndexOf('.');
		
		return fileName.substring(0, i) + generateSuffixes() + fileName.substring(i);
		
	}

	private String generateSuffixes() {
		
		if (!suffixes.isEmpty()){
			
			String str = "-" + suffixes.get(0);
			
			for (int i = 1; i < suffixes.size(); i++) {
				
				str += "-" + suffixes.get(i);
				
			}
			
			return str;
			
		}
		
		return "";
	}

	public void saveData(String name) throws IOException{
		
		SerializationHelper.write(addSuffixes(name), this.getInstances());
		
	}

	public abstract Data<T,I> createNewInstance();

	public abstract int size();

	public abstract I get(int i);

	public abstract double getClassValue(I instance);
	
	public abstract void addInstance(I instance);

	public List<Data<T, I>> split(int numberOfClassifiers) {
		
		List<Data<T, I>> instances = new ArrayList<Data<T,I>>();
		
		for (int i = 0; i < numberOfClassifiers; i++) {
			instances.add(createNewInstance());
		}
		
		Map<Double,List<I>> map = new HashMap<Double, List<I>>();
		
		for (int i = 0; i < size(); i++) {
			
			I ins = get(i);
			
			List<I> lists = map.get(getClassValue(ins));
			
			if (lists == null){
				lists = new ArrayList<I>();
				map.put(getClassValue(ins), lists);
			}
			
			lists.add(ins);
			
		}
		
		for (Entry<Double, List<I>> entry : map.entrySet()) {
			
			List<List<I>> splits = split(entry.getValue(),numberOfClassifiers);
			
			for (int i = 0; i < numberOfClassifiers; i++) {
				for (int j = 0; j < splits.get(i).size(); j++) {
					instances.get(i).addInstance(splits.get(i).get(j));
				}
			}
			
		}
		
		return instances;
		
	}
	
	private List<List<I>> split(List<I> list, int numberOfClassifiers) {
		
		Collections.shuffle(list);
		
		List<List<I>> ret = new ArrayList<List<I>>();
		
		for (int i = 0; i < numberOfClassifiers; i++) {
			
			ret.add(new ArrayList<I>());
			
		}
		
		for (int i = 0; i < list.size(); i++) {
			
			ret.get(i%numberOfClassifiers).add(list.get(i));
			
		}
		
		return ret;
	}

}
