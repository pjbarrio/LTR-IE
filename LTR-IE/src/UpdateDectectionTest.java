import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import pt.utl.ist.online.learning.engines.ElasticNetLinearPegasosEngine;
import pt.utl.ist.online.learning.engines.LinearOnlineEngine;
import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;

import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.InitialWordLoader;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.impl.FromFileInitialWordLoader;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.AdditiveFileSystemWrapping;
import edu.columbia.cs.ltrie.features.FeaturesCoordinator;
import edu.columbia.cs.ltrie.features.MatchesQueryFeatureExtractor;
import edu.columbia.cs.ltrie.features.TermFrequencyFeatureExtractor;
import edu.columbia.cs.ltrie.indexing.IndexConnector;
import edu.columbia.cs.ltrie.indexing.SimpleBooleanSimilarity;
import edu.columbia.cs.ltrie.online.svm.OnlineRankingModel;
import edu.columbia.cs.ltrie.sampling.CyclicInitialQuerySamplingTechnique;
import edu.columbia.cs.ltrie.sampling.ExplicitSamplingTechnique;
import edu.columbia.cs.ltrie.sampling.InitialQuerySamplingTechnique;
import edu.columbia.cs.ltrie.sampling.SamplingTechnique;


public class UpdateDectectionTest {
	public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException, InvalidVectorIndexException {
		//Declarations
		String path = "/home/goncalo/NYTValidationSplit/";
		File pathF = new File(path);
		int numPaths=pathF.list().length;
		String[] subPaths = new String[numPaths];
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format("%03d", i);
		}
		String extractor = "Pablo-Sub-sequences";
		String[] relationships = new String[]{"OrgAff"};

		int docsPerQuerySample = 10;
		int numQueries = 50;
		int sampleSize = 2000;
		int split = 1;

		String sampling = "Explicit";

		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationNewIndex"));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		Set<String> collectionFixed = conn.getAllFiles();
		
		for(String relationship : relationships){
			QueryParser qp = new QueryParser(Version.LUCENE_CURRENT, NYTDocumentWithFields.BODY_FIELD,  analyzer);
			String initialQueriesPath = "factCrawlFiles/initialLarge" + relationship +".txt";
			List<Query> initialQueries = loadQueries(qp, initialQueriesPath);

			Set<String> collection=new HashSet<String>(collectionFixed);
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
			
			System.out.println("Preparing feature extractors");
			FeaturesCoordinator coordinator = new FeaturesCoordinator();
			coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.TITLE_FIELD,false,false,true));
			coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.LEAD_FIELD,false,false,true));
			coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.BODY_FIELD,false,false,true));
			for(Query q : initialQueries){
				coordinator.addFeatureExtractor(new MatchesQueryFeatureExtractor(conn, q));
			}

			System.out.println("Obtaining initial sample (to use Pablo's sampling techniques)");
			
			SamplingTechnique sampler;
			if(sampling.equals("Explicit")){
				sampler = new ExplicitSamplingTechnique(path, new String[]{"001/", "002/"});
			}else if(sampling.equals("Query")){			
				sampler = new InitialQuerySamplingTechnique(conn, qp, initialQueriesPath, docsPerQuerySample,2000);
			}else if (sampling.equals("Smart")){
			
				String featSel = "ChiSquaredWithYatesCorrectionAttributeEval"; //InfoGainAttributeEval
				String extr = "SSK-"+relationship+"-SF-"+(relationship.equals("ManMadeDisaster")? "HMM":"CRF")+"-relationWords_Ranker_";
				String queryFile = "QUERIES/" + relationship + "/" + true + "/SelectedAttributes/" + extr + featSel + "_"+split+"_5000.words";
				sampler = new CyclicInitialQuerySamplingTechnique(conn, qp, queryFile, docsPerQuerySample,numQueries,sampleSize);
			}else{
				throw new UnsupportedOperationException("No sampling parameter: '" + sampling + "'");
			}
			List<String> sample = sampler.getSample();
			System.out.println("\tThe sample contains " + sample.size() + " documents.");
			collection.removeAll(sample);
			System.out.println("\tThe collection without the sample contains " + collection.size() + " documents.");

			System.out.println("Extracting information from the sample");
			List<String> relevantDocs = new ArrayList<String>();
			List<String> docs = new ArrayList<String>();
			for(String doc : sample){
				List<Tuple> tuples = extractWrapper.getTuplesDocument(doc);
				if(tuples.size()!=0){
					relevantDocs.add(doc);
				}
				docs.add(doc);
			}
			System.out.println("\tThere are " +relevantDocs.size() +" relevant documents in the sample.");
			System.out.println("\tThere are " +(docs.size()-relevantDocs.size()) +" non relevant documents in the sample.");

			System.out.println("Initial training of the ranking model");
			LinearOnlineEngine<Long> engine = new ElasticNetLinearPegasosEngine<Long>(0.1,0.99, 1, false);
			//LinearOnlineEngine<Long> engine = new LinearPegasosEngine<Long>(0.1, 1.0,false);
			OnlineRankingModel model = new OnlineRankingModel(coordinator, docs, relevantDocs, engine, 10000);
			OnlineRankingModel copyModel = model.getTempCopyModel();
			
			System.out.println(getCosineSimilarity(model.getWeightVector(),copyModel.getWeightVector()));
			
			System.out.println("Performing initial ranking");
			//Map<String,Double> scoresCollection = model.getScores(collection);
			Map<Query,Double> queryScores = model.getQueryScores();
			Map<String,Float> scoresCollection = conn.getScores(queryScores, new SimpleBooleanSimilarity(), collection);
			List<String> initialRanking = sortCollection(scoresCollection);
			List<String> rankedCollection = new ArrayList<String>(initialRanking);

			System.out.println("Extracting information");
			Map<String,Integer> relevance = new HashMap<String, Integer>();
			List<String> adaptiveRanking = new ArrayList<String>();
			int collectionSize=rankedCollection.size();
			List<String> currentBatchDocs = new ArrayList<String>();
			List<String> currentBatchRelDocs = new ArrayList<String>();
			int numUpdates=0;
			
			for(int i=0; i<collectionSize; i++){
				String doc = rankedCollection.get(i);
				collection.remove(doc);
				adaptiveRanking.add(doc);
				currentBatchDocs.add(doc);
				List<Tuple> tuples = extractWrapper.getTuplesDocument(doc);
				int num = tuples.size();
				if(num!=0){
					currentBatchRelDocs.add(doc);
					relevance.put(doc, num);
				}
				
				if(i%10==0){
					copyModel.updateModel(new HashSet<String>(currentBatchDocs), new HashSet<String>(currentBatchRelDocs), currentBatchDocs.size());
				
					currentBatchDocs = new ArrayList<String>();
					currentBatchRelDocs = new ArrayList<String>();
				}
								
				int currentNumberOfProcessedDocs = adaptiveRanking.size();
				if(currentNumberOfProcessedDocs%1000==0){
					System.out.println("Processed " + currentNumberOfProcessedDocs + " documents (" + getCosineSimilarity(model.getWeightVector(),copyModel.getWeightVector()) + ")");
				}
			}
		}
	}
	
	
	
	private static double getCosineSimilarity(Map<Long, Double> weightVector,
			Map<Long, Double> weightVector2) {
		Set<Long> commonKeys = new HashSet<Long>(weightVector.keySet());
		commonKeys.retainAll(weightVector2.keySet());
		
		double inner = 0;
		for(Long key : commonKeys){
			double val1 = weightVector.get(key);
			double val2 = weightVector2.get(key);
			inner+=val1*val2;
		}
		
		double normSq1 = 0;
		for(Entry<Long,Double> entry : weightVector.entrySet()){
			normSq1+=entry.getValue()*entry.getValue();
		}
		
		double normSq2 = 0;
		for(Entry<Long,Double> entry : weightVector2.entrySet()){
			normSq2+=entry.getValue()*entry.getValue();
		}
		
		return inner/Math.sqrt(normSq1*normSq2);
	}



	private static List<Query> loadQueries(QueryParser qp, String queryFile) throws ParseException, IOException {
		InitialWordLoader iwl = new FromFileInitialWordLoader(qp,queryFile);
		List<Query> words = iwl.getInitialQueries();
		return words;
	}
	
	private static List<String> sortCollection(Map<String,Float> scoresCollection){
		final Map<String,Float> scores = scoresCollection;
		List<String> collection = new ArrayList<String>(scoresCollection.keySet());
		Collections.sort(collection, new Comparator<String>() {
			@Override
			public int compare(String doc1, String doc2) {
				Float score2 = scores.get(doc2);
				if(score2==null){
					score2=0.0f;
				}
				Float score1 = scores.get(doc1);
				if(score1==null){
					score1=0.0f;
				}
				return (int) Math.signum(score2-score1);
			}
		});
		return collection;
	}
}
