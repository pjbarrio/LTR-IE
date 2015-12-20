package edu.columbia.cs.ltrie.sampling.execute;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import com.nytlabs.corpus.NYTCorpusDocument;
import com.nytlabs.corpus.NYTCorpusDocumentParser;

import edu.columbia.cs.ltrie.datamodel.DocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.AdditiveFileSystemWrapping;
import edu.columbia.cs.ltrie.indexing.IndexConnector;
import edu.columbia.cs.ltrie.sampling.CyclicInitialQuerySamplingTechnique;
import edu.columbia.cs.ltrie.sampling.SamplingTechnique;
import edu.columbia.cs.ltrie.utils.SerializationHelper;

public class CreateSample {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws ClassNotFoundException 
	 */

	public static void main(String[] args) throws ParseException, IOException, ClassNotFoundException {

		//Declarations
		String path = args[0];
		
		int rel = Integer.valueOf(args[1]);
		
		int numPaths=672;
		String[] subPaths = new String[numPaths];
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format("%03d", i);
		}
		
		int sampleSize = 2000;

		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		Directory directory = new RAMDirectory();
		IndexConnector conn = new IndexConnector(analyzer, directory, "");

		Set<String> collection = indexCollection(path,subPaths,conn);
		
		String[][] relationships= new String[][]{{"ManMadeDisaster","HMM"},{"PersonCareer","CRF"},{"VotingResult","CRF"},{"Indictment-Arrest-Trial","CRF"}};
		
		String[][] extractors = {{"Pablo-Sub-sequences","SSK"}};
		
		boolean[] stopWordsA = {true,false};
		
		for (int ext = 0; ext < extractors.length; ext++) {
			
			String extractor = extractors[ext][0];
			String modelExt = extractors[ext][1];
			
//			for (int rel = 0; rel < relationships.length; rel++) {
				
				String relationship = relationships[rel][0];
				String model = relationships[rel][1];
				String resultsPath = "results" + relationship;
				
				System.out.println("Initiating IE programs");
				AdditiveFileSystemWrapping extractWrapper = new AdditiveFileSystemWrapping();
				for(String subPath : subPaths){
					if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
						extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationship + ".data");
					}else{
						extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + ".data");
					}
				}
				
				for (int split = 1; split <= 5; split++) { //should be 1 <= 5
					
					for (int numQueries = 500; numQueries > 0 ; numQueries-=50){ //should be 1000
						
						for (int docsPerQuery = 50; docsPerQuery > 0; docsPerQuery-=10) { //should be 100
							
							for (int i = 0; i < stopWordsA.length; i++) {
								
								boolean stopWords = stopWordsA[i];
								
								String prefix = "samples/" + relationship + "/" + modelExt + "/" + split + "/" + stopWords + "/";
								
								new File(prefix).mkdirs();
								
								String name = "NQ" + numQueries + "-DPQ" + docsPerQuery;
								
								if (new File(prefix,name+ ".relevant").exists())
									continue;
								
								String extr = modelExt + "-"+relationship+"-SF-"+model+"-relationWords_Ranker_"; //SSK-"+relationship+"-SF-CRF-relationWords_Ranker_
								
								String featSel = "ChiSquaredWithYatesCorrectionAttributeEval"; //InfoGainAttributeEval
								
								String queryFile = "QUERIES/" + relationship + "/" + stopWords + "/SelectedAttributes/" + extr + featSel + "_"+split+"_5000.words";
								
								System.out.println("Obtaining initial sample (to use Pablo's sampling techniques)");
								//SamplingTechnique sampler = new ExplicitSamplingTechnique(path, new String[]{"001/", "002/"});
								QueryParser qp = new QueryParser(Version.LUCENE_CURRENT, NYTDocumentWithFields.BODY_FIELD,  analyzer);
								
								SamplingTechnique sampler = new CyclicInitialQuerySamplingTechnique(conn, qp, queryFile, docsPerQuery,numQueries,sampleSize);
								List<String> sample = sampler.getSample();
								System.out.println("\tThe sample contains " + sample.size() + " documents.");
								//collection.removeAll(sample);
								//System.out.println("\tThe collection without the sample contains " + collection.size() + " documents.");

								System.out.println("Extracting information from the sample");
								Set<String> relevantDocs = new HashSet<String>();
								for(String doc : sample){
									if(extractWrapper.getNumTuplesDocument(doc)!=0){
										relevantDocs.add(doc);
									}

								}
								
								SerializationHelper.write(new File(prefix,name + ".relevant").getAbsolutePath(), relevantDocs);
								SerializationHelper.write(new File(prefix,name + ".sample").getAbsolutePath(), sample);

							}
							
						}
						
					}
					
				}
				
//			}
			
		}
		
	}

	private static Set<String> indexCollection(String path, String[] subPaths, IndexConnector conn) throws IOException {
		Set<String> docs = new HashSet<String>();
		for(String p : subPaths){
			docs.addAll(indexDocs(path + p, conn));
		}
		return docs;
	}
	
	private static List<String> indexDocs(String path, IndexConnector conn) throws IOException{
		System.out.println("Load Documents in " + path);
		List<String> docs = new ArrayList<String>();
		NYTCorpusDocumentParser parser = new NYTCorpusDocumentParser();
		List<DocumentWithFields> documentsWithFields = new ArrayList<DocumentWithFields>();
		File fDir = new File(path);
		for(File f : fDir.listFiles()){
			NYTCorpusDocument doc = parser.parseNYTCorpusDocumentFromFile(f, false);
			documentsWithFields.add(new NYTDocumentWithFields(doc));
			docs.add(f.getName());
		}
		
		System.out.println("Load Index");
		
		for(DocumentWithFields docFields : documentsWithFields){
			conn.addDocument(docFields);
		}
		conn.closeWriter();
		return docs;
	}
	
}
