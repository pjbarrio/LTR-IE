package edu.columbia.cs.ltrie;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.InitialWordLoader;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.impl.FromFileInitialWordLoader;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.CompressedAdditiveFileSystemWrapping;
import edu.columbia.cs.ltrie.features.AllFieldsTermFrequencyFeatureExtractor;
import edu.columbia.cs.ltrie.features.FeaturesCoordinator;
import edu.columbia.cs.ltrie.features.MatchesQueryFeatureExtractor;
import edu.columbia.cs.ltrie.features.TermFrequencyFeatureExtractor;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class GenerateInitialFeatureVectors {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException {
		String path = "/home/goncalo/NYTValidationSplit/";
		String outputFolder = "svmRankFiles/";

		File pathF = new File(path);
		int numPaths=pathF.list().length;
		String[] subPaths = new String[numPaths];
		String folderDesign = "%0" + String.valueOf(numPaths).length() + "d";
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format(folderDesign, i);
		}
		String extractor = "Pablo-Sub-sequences";

		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationRealIndex"));
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationNewIndex"));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		Set<String> collectionFixed = conn.getAllFiles();


		String[] fieldsVector = new String[]{NYTDocumentWithFields.TITLE_FIELD,
				NYTDocumentWithFields.LEAD_FIELD,NYTDocumentWithFields.BODY_FIELD};
		QueryParser qp = new MultiFieldQueryParser(
				Version.LUCENE_41, 
				fieldsVector,
				new StandardAnalyzer(Version.LUCENE_41));
		String featSel = "ChiSquaredWithYatesCorrectionAttributeEval"; //InfoGainAttributeEval

		for(String relationship : new String[]{"NaturalDisaster","ManMadeDisaster", "OrgAff", "Outbreaks", "PersonCareer",
				"Indictment-Arrest-Trial","VotingResult"}){

			String resultsPath = "results" + relationship;
			CompressedAdditiveFileSystemWrapping extractWrapper = new CompressedAdditiveFileSystemWrapping();
			for(String subPath : subPaths){
				if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationship + ".data");
				}else{
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + ".data");
				}
			}


			FeaturesCoordinator coordinator = new FeaturesCoordinator();
			Set<String> fields = new HashSet<String>();
			for(String field : fieldsVector){
				fields.add(field);
			}
			List<Query> initialQueries = loadQueries(qp, "factCrawlFiles/initial" + relationship +".txt");
			coordinator.addFeatureExtractor(new AllFieldsTermFrequencyFeatureExtractor(conn, fields, false,false,true));
			//coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.TITLE_FIELD,false,false,true));
			//coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.LEAD_FIELD,false,false,true));
			//coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.BODY_FIELD,false,false,true));
			for(Query q : initialQueries){
				coordinator.addFeatureExtractor(new MatchesQueryFeatureExtractor(conn, q));
			}

			System.out.println("Starting");
			int i=1;
			int size=collectionFixed.size();
			List<String> documents = new ArrayList<String>();
			Map<String,Map<Long,Double>> vectors = new HashMap<String, Map<Long,Double>>();
			Map<String,Boolean> labels = new HashMap<String, Boolean>();
			for(String doc : collectionFixed){
				documents.add(doc);
				vectors.put(doc,coordinator.getFeatures(doc));
				labels.put(doc,extractWrapper.getNumTuplesDocument(doc)!=0);

				if(i==11000){
					dump(documents.subList(10000, 11000),vectors,labels,outputFolder+relationship+".test");
					break;
				}

				if(i%1000==0){
					dump(documents,vectors,labels,outputFolder+relationship+i+".train");
				}

				i++;
			}
		}
	}

	private static List<Query> loadQueries(QueryParser qp, String queryFile) throws ParseException, IOException {
		InitialWordLoader iwl = new FromFileInitialWordLoader(qp,queryFile);
		List<Query> words = iwl.getInitialQueries();
		return words;
	}

	private static void dump(List<String> documents, Map<String,Map<Long,Double>> vectors,
			Map<String,Boolean> labels, String filename) throws IOException{
		System.out.println("Dumping " + documents.size() + " vectors to " + filename);
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)));
		for(String doc : documents){
			dumpVector(doc, vectors.get(doc),labels.get(doc),bw);
		}
		bw.close();
	}

	private static void dumpVector(String doc, Map<Long, Double> vector, boolean label, BufferedWriter bw) throws IOException {
		List<Long> indexes = new ArrayList<Long>(vector.keySet());
		Collections.sort(indexes);
		StringBuffer buf = new StringBuffer();
		if(label){
			buf.append(1);
		}else{
			buf.append(0);
		}
		buf.append(" qid:1");
		for(long l : indexes){
			buf.append(" " + l + ":" + (vector.get(l).floatValue()));
		}

		buf.append(" # " + doc);
		bw.append(buf.toString());
		bw.newLine();
	}

}
