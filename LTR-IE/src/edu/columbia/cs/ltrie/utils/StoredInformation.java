package edu.columbia.cs.ltrie.utils;
import java.io.Serializable;
import java.util.List;

import edu.columbia.cs.ltrie.datamodel.Tuple;


public class StoredInformation implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1674713394189035410L;
	private String extractor;
	private String relationship;
	private String pathFile;
	private List<Tuple> tuples;
	
	public StoredInformation(String extractor, String relationship, String pathFile, List<Tuple> tuples){
		this.extractor=extractor;
		this.relationship=relationship;
		this.pathFile=pathFile;
		this.tuples=tuples;
	}

	public String getExtractor() {
		return extractor;
	}

	public void setExtractor(String extractor) {
		this.extractor = extractor;
	}

	public String getRelationship() {
		return relationship;
	}

	public void setRelationship(String relationship) {
		this.relationship = relationship;
	}

	public String getPathFile() {
		return pathFile;
	}

	public void setPathFile(String pathFile) {
		this.pathFile = pathFile;
	}

	public int getSize() {
		return tuples.size();
	}
	
	public List<Tuple> getTuples(){
		return tuples;
	}
}
