package edu.columbia.cs.ltrie.extractor;


import java.io.IOException;
import java.util.List;

import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.utils.DocumentLoader;

public abstract class ExtractionSystem {
	
	public abstract List<Tuple> execute(String path, String docContent);
	
	public List<Tuple> extractTuplesFrom(String path) throws IOException{
		String content = DocumentLoader.loadDocument(path);
		List<Tuple> tuples = execute(path, content);
			
		return tuples;
	}

	public List<Tuple> extractTuplesFrom(String path, String content) throws IOException{

		List<Tuple> tuples = execute(path, content);
			
		return tuples;
	}
	
	public abstract String getPlanString();
	
	public String toString(){
		return getPlanString();
	}
}
