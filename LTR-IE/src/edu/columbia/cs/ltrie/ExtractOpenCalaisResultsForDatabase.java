package edu.columbia.cs.ltrie;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import prototype.CIMPLE.utils.CPUTimeMeasure;
import pt.utl.ist.online.learning.utils.EmptyStatistics;
import pt.utl.ist.online.learning.utils.TimeMeasurer;

import com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException;

import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.extractor.impl.opencalais.RDFPESExtractor;
import edu.columbia.cs.ltrie.extractor.impl.reel.REELRelationExtractionSystem;
import edu.columbia.cs.ltrie.utils.SerializationHelper;
import edu.columbia.cs.ltrie.utils.StoredInformation;

public class ExtractOpenCalaisResultsForDatabase {

	public static void main(String[] args) throws IllegalArgumentException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, SQLException {

		String task = args[0];

		String filespath = getFilesPath(task);

		String filespathFixed = getFilesPathFixed(task);

		File[] filesFixed = new File(filespathFixed).listFiles();
		
		String extractor = "OpenCalais";

		Map<String, List<StoredInformation>> relMap = new HashMap<String, List<StoredInformation>>();

		File[] files = new File(filespath).listFiles();
		int numFiles = files.length;
		for(int i=0; i<numFiles; i++){
			File doc = files[i];

			BufferedReader br = new BufferedReader(new FileReader(doc));

			String line = br.readLine();

			br.close();

			if (line.startsWith("<Error")){ //need to look in Fixed

				System.out.println("Checking in Fixed for: " + doc.getName());
				
				Map<String, List<Tuple>> tupls = new HashMap<String, List<Tuple>>();
				
				for (int j = 0; j < filesFixed.length; j++) {
					
					File docF = filesFixed[j];
					
					if (docF.getName().contains(doc.getName())){
						
						br = new BufferedReader(new FileReader(docF));

						line = br.readLine();

						br.close();
						
						String[] spls = line.split("<!--Relations: ");

						if (spls.length > 1){
							String[] rels = spls[1].split(", ");
							for (String relationship : rels) {

								List<Tuple> t = RDFPESExtractor.extract(docF.toURI(),relationship, doc.getName());
								
								if (!t.isEmpty()){
								
									List<Tuple> toAdd = tupls.get(relationship);
									
									if (toAdd == null){
										toAdd = new ArrayList<Tuple>();
										tupls.put(relationship, toAdd);
									}

									toAdd.addAll(t);
									
								}
									
							}
						}
						
					}
					
				}
				
				for (Entry<String, List<Tuple>> entries : tupls.entrySet()) {
					getListInfo(relMap, entries.getKey()).add(new StoredInformation(extractor, entries.getKey(), doc.getName(),entries.getValue()));
				}
								
			}else{

				String[] spls = line.split("<!--Relations: ");

				if (spls.length > 1){
					String[] rels = spls[1].split(", ");
					for (String relationship : rels) {

						List<Tuple> t = RDFPESExtractor.extract(doc.toURI(),relationship, doc.getName());
						if(t.size()!=0){
							getListInfo(relMap, relationship).add(new StoredInformation(extractor, relationship, doc.getName(),t));
						}

					}
				}
			}		
			if (i%1000 == 0)
				System.out.println((i*100)/numFiles + "% of the documents processed!");

		}

		storeResults(relMap, task, extractor);

	}


	private static void storeResults(Map<String, List<StoredInformation>> relMap, String task, String extractor) throws IOException {

		for (Entry<String, List<StoredInformation>> entry : relMap.entrySet()) {

			String outputFile = "results" + entry.getKey() + "/" + task + "_" + extractor + "_" + entry.getKey() + ".data";

			new File("results" + entry.getKey() + "/").mkdirs();

			storeResults(entry.getValue(), outputFile);

		}

	}

	private static List<StoredInformation> getListInfo(
			Map<String, List<StoredInformation>> relMap, String relation) {

		List<StoredInformation> ret = relMap.get(relation);

		if (ret == null){
			ret = new ArrayList<StoredInformation>();
			relMap.put(relation, ret);
		}

		return ret;

	}


	public static String getFilesPath(String task) {

		if ("Validation".equals(task)){
			return "/local/pjbarrio/Files/Downloads/NYTValidationSplitPlainExtraction/";
		} else if ("Train".equals(task)){
			return "/local/pjbarrio/Files/Downloads/NYTTrainExtraction/";
		} else if ("Test".equals(task)){
			return "/local/pjbarrio/Files/Downloads/NYTTest/NYTTestExtraction/";
		}

		return null;

	}

	public static String getFilesPathFixed(String task) {
		if ("Validation".equals(task)){
			return "/local/pjbarrio/Files/Downloads/NYTValidationSplitPlainFixed/";
		} else if ("Train".equals(task)){
			return "/local/pjbarrio/Files/Downloads/NYTTrainExtractionFixed/";
		} else if ("Test".equals(task)){
			return "/local/pjbarrio/Files/Downloads/NYTTest/NYTTestFixed/";
		}

		return null;
	}

	private static void storeResults(List<StoredInformation> listInfo, String path) throws IOException {

		SerializationHelper.write(path, listInfo);

	}
}
