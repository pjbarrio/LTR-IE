package edu.columbia.cs.ltrie;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.google.gdata.util.common.base.Pair;

import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.extractor.impl.opencalais.RDFPESExtractor;
import edu.columbia.cs.ltrie.utils.SerializationHelper;
import edu.columbia.cs.ltrie.utils.StoredInformation;
import edu.stanford.nlp.ie.machinereading.structure.Span;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class TuplesToSentencesforOC {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {

		int splits = 100;
		
		String[] subPaths = new String[splits];
		String folderDesign = "%0" + String.valueOf(splits).length() + "d";
		for(int i=1; i<=splits; i++){
			subPaths[i-1]=String.format(folderDesign, i);
		}
		
		String task = args[0];

		String extractor = "OpenCalais";

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		String path = ExtractOpenCalaisResultsForDatabase.getFilesPath(task); 
		String fixedPath = ExtractOpenCalaisResultsForDatabase.getFilesPathFixed(task);

		System.out.println("Load Documents in " + path);

		System.out.println("Load Index");		

		File fDir = new File(path);

		File[] Allfiles = fDir.listFiles();

		String[] filesFixed = new File(fixedPath).list();

		Arrays.sort(filesFixed);

		int count = 0;

		double incs = (double)Allfiles.length/(double)splits;
		
		for (int sp = 0; sp < splits; sp++) {
			
			File[] files = Arrays.copyOfRange(Allfiles, (int)(sp*incs), (int)((sp+1)*incs));
			
			Map<String, List<StoredInformation>> relMap = new HashMap<String, List<StoredInformation>>();

			for(File f : files){

				List<List<Span>> sentences = new ArrayList<List<Span>>();

				List<Map<String,List<Pair<Tuple,Integer>>>> tuples = new ArrayList<Map<String,List<Pair<Tuple,Integer>>>>();

				String content = RDFPESExtractor.extractContent(f.toURI());

				if (content == null){ //Error

					for (int i = 0; i < filesFixed.length; i++) {

						if (filesFixed[i].contains(f.getName())){

							File ff = new File(fixedPath,filesFixed[i]);

							String scont = RDFPESExtractor.extractContent(ff.toURI());

							if (scont != null){

								Annotation document = new Annotation(scont);

								// run all Annotators on this text
								pipeline.annotate(document);

								// these are all the sentences in this document
								// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
								List<CoreMap> ss = document.get(SentencesAnnotation.class);
								List<Span> toadd = new ArrayList<Span>(ss.size());

								for (CoreMap sentence : ss) {
									toadd.add(new Span(sentence.get(CharacterOffsetBeginAnnotation.class), sentence.get(CharacterOffsetEndAnnotation.class)));
								}

								sentences.add(toadd);

								Map<String,List<Pair<Tuple,Integer>>> tts = new HashMap<String, List<Pair<Tuple,Integer>>>();

								BufferedReader br = new BufferedReader(new FileReader(ff));

								String line = br.readLine();

								br.close();

								String[] spls = line.split("<!--Relations: ");

								if (spls.length > 1){ //work only if it has extractions

									String[] relations = spls[1].split(", ");

									for (String relation : relations) {

										List<Pair<Tuple,Integer>> tups = RDFPESExtractor.extractWithOffset(ff.toURI(), relation, f.getName());

										tts.put(relation, tups);

									}

								}

								tuples.add(tts);



							}


						}

					}

				} else {

					//work only if it has extractions

					Annotation document = new Annotation(content);

					// run all Annotators on this text
					pipeline.annotate(document);

					// these are all the sentences in this document
					// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
					List<CoreMap> ss = document.get(SentencesAnnotation.class);
					List<Span> toadd = new ArrayList<Span>(ss.size());

					for (CoreMap sentence : ss) {
						toadd.add(new Span(sentence.get(CharacterOffsetBeginAnnotation.class), sentence.get(CharacterOffsetEndAnnotation.class)));
					}

					sentences.add(toadd);

					Map<String,List<Pair<Tuple,Integer>>> tts = new HashMap<String, List<Pair<Tuple,Integer>>>();

					BufferedReader br = new BufferedReader(new FileReader(f));

					String line = br.readLine();

					br.close();

					String[] spls = line.split("<!--Relations: ");

					if (spls.length > 1){

						String[] relations = spls[1].split(", ");

						for (String relation : relations) {

							List<Pair<Tuple,Integer>> tups = RDFPESExtractor.extractWithOffset(f.toURI(), relation, f.getName());

							tts.put(relation, tups);

						}

					}
					
					tuples.add(tts);


				}

				Map<String,Map<String,List<Tuple>>> sentRelTupMap = new HashMap<String, Map<String,List<Tuple>>>();

				int sent = 1;

				for (int i = 0; i < sentences.size(); i++) {

					List<Span> sents = sentences.get(i);

					Map<String, List<Pair<Tuple, Integer>>> tupls = tuples.get(i);

					for (int j = 0; j < sents.size(); j++) {

						for (Entry<String, List<Pair<Tuple, Integer>>> tupsRel : tupls.entrySet()) {

							for (Pair<Tuple, Integer> tup : tupsRel.getValue()) {

								int offset = tup.getSecond();

								if (sents.get(j).contains(offset)){

									String fnam = f.getName() + "." + sent;
									Tuple t = tup.getFirst();
									String relation = tupsRel.getKey();

									Map<String, List<Tuple>> aux = sentRelTupMap.get(fnam);
									if (aux == null){
										aux = new HashMap<String, List<Tuple>>();
										sentRelTupMap.put(fnam, aux);
									}

									List<Tuple> taux = aux.get(relation);
									if (taux == null){
										taux = new ArrayList<Tuple>();
										aux.put(relation, taux);
									}

									taux.add(t);

								} 

							}

						}

						sent++;

					}

				}

				for (Entry<String, Map<String, List<Tuple>>> entry : sentRelTupMap.entrySet()) {

					String senti = entry.getKey();

					for (Entry<String, List<Tuple>> ent : entry.getValue().entrySet()) {

						getListInfo(relMap, ent.getKey()).add(new StoredInformation(extractor, ent.getKey(), senti,ent.getValue()));

					}

				}

				if (count%1000 == 0)
					System.out.println(count + " documents processed!");


				count++;

			}

			storeResults(subPaths[sp], relMap, task, extractor);
			
			relMap.clear();
			
			System.gc();
			
		}
		
		

	}


	private static void storeResults(String subpath, Map<String, List<StoredInformation>> relMap, String task, String extractor) throws IOException {

		for (Entry<String, List<StoredInformation>> entry : relMap.entrySet()) {

			String outputFile = "results" + entry.getKey() + "/" + subpath + "_" + task + "_" + extractor + "_" + entry.getKey() + "-sentence.data";

			new File("results" + entry.getKey() + "/").mkdirs();

			storeResults(entry.getValue(), outputFile);

		}

	}

	private static void storeResults(List<StoredInformation> listInfo, String path) throws IOException {
		SerializationHelper.write(path, listInfo);
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

}
