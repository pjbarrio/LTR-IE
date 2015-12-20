package edu.columbia.cs.ltrie.extractor.wrapping.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.extractor.wrapping.ExtractionWrapper;
import edu.columbia.cs.ltrie.utils.SerializationHelper;
import edu.columbia.cs.ltrie.utils.StoredInformation;

public class CompressedAdditiveFileSystemWrapping implements ExtractionWrapper {
	
	private Map<String,byte[]> tuples = new HashMap<String, byte[]>();
	
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
	
	public Collection<String> getAllDocs() {
		return tuples.keySet();
	}
	
	private void loadInformation(List<StoredInformation> listInfo) throws IOException{
		for(StoredInformation info : listInfo){
			tuples.put(info.getPathFile(),compressToByteArray(info.getTuples()));
		}
	}
	
	private byte[] compressToByteArray(List<Tuple> tuples) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
		ObjectOutputStream objectOut = new ObjectOutputStream(gzipOut);
		objectOut.writeObject(tuples);
		objectOut.close();
		byte[] bytes = baos.toByteArray();
		return bytes;
	}
	
	private List<Tuple> loadFromByteArray(byte[] bytes) throws IOException, ClassNotFoundException{
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		GZIPInputStream gzipIn = new GZIPInputStream(bais);
		ObjectInputStream objectIn = new ObjectInputStream(gzipIn);
		List<Tuple> myObj = (List<Tuple>) objectIn.readObject();
		objectIn.close();
		return myObj;
	}

	@Override
	public int getNumTuplesDocument(String doc) throws IOException {
		File f = new File(doc);
		byte[] bytes = tuples.get(f.getName());
		if(bytes==null){
			return 0;
		}
		List<Tuple> t = null;
		try {
			t = loadFromByteArray(bytes);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return t.size();
	}

	@Override
	public List<Tuple> getTuplesDocument(String doc) throws IOException {
		File f = new File(doc);
		
		
		byte[] bytes = tuples.get(f.getName());
		if(bytes==null){
			return new ArrayList<Tuple>();
		}
		List<Tuple> t = null;
		try {
			t = loadFromByteArray(bytes);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return t;
	}
	
	public int getNumRelevantDocuments(){
		return tuples.size();
	}

}
