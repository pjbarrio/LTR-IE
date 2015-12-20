package edu.columbia.cs.ltrie.sampling.queries.generation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.mitre.jawb.io.SgmlDocument;
//import org.mitre.jawb.io.SgmlDocument;

import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.extractor.ExtractionSystem;
import edu.columbia.cs.ltrie.extractor.impl.cimple.CIMPLEExtractionSystem;
import edu.columbia.cs.ltrie.utils.SerializationHelper;

public class CreateExtractionSplits {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalArgumentException 
	 */
	public static void main(String[] args) throws IOException, IllegalArgumentException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
		
///		String path = "/proj/db-files2/NoBackup/pjbarrio/Dataset/TREC/CleanCollection";
		
		String ieExtractionPlan = "CIMPLEExecutionPlans/" + args[0] + "Plain.plan";
		
		ExtractionSystem ieSystem = new CIMPLEExtractionSystem(ieExtractionPlan);
		
///		int split = Integer.valueOf(args[1]);
		
///		int dataSplit = Integer.valueOf(args[2]);
		
///		List<String> files = FileUtils.readLines(new File(path + "/files" + split + ".txt"));

		List<String> files = FileUtils.readLines(new File("extractionSplits/diseaseOutbreak.uf"));
		
		double d = (double)files.size() / 5.0;
		
///		int first = (int)Math.floor(dataSplit * d);

///		int last ;
///		
///		if (dataSplit <4)
///			last = (int)Math.floor(first + d);
///		else
///			last = files.size();
		
		
///		files = files.subList(first, last);
		
		List<String> usefuls = new ArrayList<String>();
		List<String> useless = new ArrayList<String>();
		
		Map<String,List<Tuple>> map = new HashMap<String,List<Tuple>>();
		
		for (int i = 0; i < files.size(); i++) {
		
			if (i % 1000 == 0)
				System.out.print(".");
			
			if (i % 10000 == 0)
				System.out.println(".");
			
			
//			File f = new File(path,files.get(i).substring(2));

			File f = new File(files.get(i));
			
			if (f.isDirectory())
				continue;
			
			String content = getContent(f);
			
			List<Tuple> t = ieSystem.extractTuplesFrom(f.getAbsolutePath(), content);
			
			if (t != null && !t.isEmpty()){
				usefuls.add(f.getAbsolutePath());
				map.put(f.getAbsolutePath(), t);
			} else {
				useless.add(f.getAbsolutePath());
			}
			
		}

		SerializationHelper.write("extractionSplits/" + args[0] +".ser", map);
		
///		FileUtils.writeLines(new File("extractionSplits/" + split + "-" +dataSplit+ args[0] + "useful.txt"), usefuls);
///		FileUtils.writeLines(new File("extractionSplits/" + split + "-" +dataSplit+ args[0] + "useless.txt"), useless);

		//After this, I combine all the outputs
		// Then do a shuffle
		// Then do splits of 5000 documents.
		
	}

	public static String getContent(File f) {
		
		try {

			FileReader fr = new FileReader(f.getAbsolutePath());
			
			String content = new SgmlDocument(fr).getSignalText();
			
			fr.close();
			
			return content;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
		return null;
	}

}
