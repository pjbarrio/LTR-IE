package edu.columbia.cs.ltrie.extractor.wrapping.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.columbia.cs.ltrie.datamodel.Span;
import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.extractor.wrapping.ExtractionWrapper;
import edu.columbia.cs.ltrie.utils.SerializationHelper;
import edu.columbia.cs.ltrie.utils.StoredInformation;

public class AdditiveFileSystemWrapping implements ExtractionWrapper {
	
	private Map<String,List<Tuple>> tuples = new HashMap<String, List<Tuple>>();
	private final String EMPTY_STRING = "";
	
	public void addFiles(String filePath) throws IOException, ClassNotFoundException{
		File f = new File(filePath);
		if(f.isDirectory()){
			File[] listFiles = f.listFiles(new FileFilter() {
				
				@Override
				public boolean accept(File pathname) {
					
					return pathname.getName().endsWith("data");
					
				}
			});
			for(File file : listFiles){
				List<StoredInformation> listInfo = (List<StoredInformation>) SerializationHelper.read(file.getAbsolutePath());
				loadInformation(listInfo);
			}
		}else{
			List<StoredInformation> listInfo = (List<StoredInformation>) SerializationHelper.read(f.getAbsolutePath());
			loadInformation(listInfo);
		}
	}
	
	private void loadInformation(List<StoredInformation> listInfo) throws IOException{
		for(StoredInformation info : listInfo){
			List<Tuple> tups = info.getTuples();
			for(Tuple t : tups){
				for(String field : t.getFieldNames()){
					Span s = t.getData(field);
					s.setDoc(EMPTY_STRING);
				}
			}
			tuples.put(info.getPathFile(),tups);
		}
	}

	@Override
	public int getNumTuplesDocument(String doc) throws IOException {
		File f = new File(doc);
		List<Tuple> t = tuples.get(f.getName());
		if(t==null){
			return 0;
		}
		return t.size();
	}

	@Override
	public List<Tuple> getTuplesDocument(String doc) throws IOException {
		File f = new File(doc);
		List<Tuple> t = tuples.get(f.getName());
		if(t==null){
			return new ArrayList<Tuple>();
		}
		return t;
	}
	
	public int getNumRelevantDocuments(){
		return tuples.size();
	}

	public Map<String, List<Tuple>> getInternal() {
		return tuples;
	}

}
