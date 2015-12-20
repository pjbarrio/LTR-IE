package edu.columbia.cs.ltrie.datamodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DocumentWithFields {
	public static final String PATH_FIELD = "PATH";
	
	private String path;
	private Map<String,List<String>> fields;
	
	public DocumentWithFields(String path){
		this.path=path;
		this.fields = new HashMap<String, List<String>>();
		addPathField(path);
	}
	
	public void addField(String field, String newData) throws UnsupportedFieldNameException{
		if(field.equals(PATH_FIELD)){
			throw new UnsupportedFieldNameException("'" + PATH_FIELD + "' is a reserved field name for the path of the file. Please change it.");
		}
		List<String> currentFieldData = fields.get(field);
		if(currentFieldData==null){
			currentFieldData=new ArrayList<String>();
		}
		
		currentFieldData.add(newData);
		fields.put(field, currentFieldData);
	}
	
	private void addPathField(String path){
		List<String> pathList = new ArrayList<String>();
		pathList.add(path);
		fields.put(PATH_FIELD, pathList);
	}
	
	public List<String> getField(String field){
		return fields.get(field);
	}
	
	public Set<String> getFields(){
		return fields.keySet();
	}
	
	public String getPath(){
		return path;
	}
}
