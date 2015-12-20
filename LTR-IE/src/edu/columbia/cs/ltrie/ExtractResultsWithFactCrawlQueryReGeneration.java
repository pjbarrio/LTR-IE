package edu.columbia.cs.ltrie;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

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

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.MemoryEfficientHashMap;
import pt.utl.ist.online.learning.utils.MemoryEfficientHashSet;
import pt.utl.ist.online.learning.utils.TimeMeasurer;

import com.nytlabs.corpus.NYTCorpusDocument;
import com.nytlabs.corpus.NYTCorpusDocumentParser;

import edu.columbia.cs.ltrie.baseline.factcrawl.RFACT;
import edu.columbia.cs.ltrie.baseline.factcrawl.graph.QMDGraph;
import edu.columbia.cs.ltrie.baseline.factcrawl.graph.scores.FactCrawlQMDScores;
import edu.columbia.cs.ltrie.baseline.factcrawl.graph.scores.QMDScores;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.FromWeightedFileInitialWordLoader;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.InitialWordLoader;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.impl.FromFileInitialWordLoader;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.QueryGenerationMethod;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.AllDocumentsQueryQGM;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.AttributeValuesQGM;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.FactTypeNameQGM;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.FeatureExtractorBasedQueryGenerationMethod;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.InitialQueriesQGM;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.LexicoSyntacticFactTypeQGM;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.SignificantPhrasesQGM;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.utils.FeaturesRanker;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.utils.ScoresCombinator;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.utils.impl.SumScoreCombinator;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.utils.impl.ValueBasedFeatureRanker;
import edu.columbia.cs.ltrie.baseline.factcrawl.strategy.ExtractionStrategy;
import edu.columbia.cs.ltrie.baseline.factcrawl.strategy.impl.AdaptiveExtractionStrategy;
import edu.columbia.cs.ltrie.baseline.factcrawl.strategy.impl.RegenerationQueriesStrategy;
import edu.columbia.cs.ltrie.baseline.factcrawl.strategy.impl.StaticExtractionStrategy;
import edu.columbia.cs.ltrie.datamodel.DocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.excel.ExcelGenerator;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.AdditiveFileSystemWrapping;
import edu.columbia.cs.ltrie.features.AllFieldsTermFrequencyFeatureExtractor;
import edu.columbia.cs.ltrie.features.FeatureExtractor;
import edu.columbia.cs.ltrie.features.FeaturesCoordinator;
import edu.columbia.cs.ltrie.features.MatchesQueryFeatureExtractor;
import edu.columbia.cs.ltrie.features.TermFrequencyFeatureExtractor;
import edu.columbia.cs.ltrie.indexing.IndexConnector;
import edu.columbia.cs.ltrie.sampling.CyclicInitialQuerySamplingTechnique;
import edu.columbia.cs.ltrie.sampling.ExplicitSamplingTechnique;
import edu.columbia.cs.ltrie.sampling.InitialQuerySamplingTechnique;
import edu.columbia.cs.ltrie.sampling.SamplingTechnique;
import edu.columbia.cs.ltrie.updates.ExactWindowUpdateDecision;
import edu.columbia.cs.ltrie.updates.FeatureRankOnline;
import edu.columbia.cs.ltrie.updates.FeatureShiftOnline;
import edu.columbia.cs.ltrie.updates.ModelSimilarityUpdateDecision;
import edu.columbia.cs.ltrie.updates.UpdateDecision;
import edu.columbia.cs.ltrie.updates.UpdatePrediction;
import edu.columbia.cs.ltrie.utils.SerializationHelper;

public class ExtractResultsWithFactCrawlQueryReGeneration {
	public static void main(String[] args) throws IOException, ClassNotFoundException, InvalidVectorIndexException, RowsExceededException, WriteException, ParseException {
		//Declarations
		TimeMeasurer measurer = new TimeMeasurer();
		String updateMethod = args[7];
		String path = args[0];
		//String path = "/home/goncalo/NYTValidationSplit/";
		File pathF = new File(path);
		int numPaths=pathF.list().length;
		String[] subPaths = new String[numPaths];
		String folderDesign = "%0" + String.valueOf(numPaths).length() + "d";
		HashMap<String,String> pathDictionary = new HashMap<String,String>();
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format(folderDesign, i);
			String pathChildren = path + subPaths[i-1];
			File f = new File(pathChildren);
			for(File c : f.listFiles()){
				pathDictionary.put(c.getName(),c.getAbsolutePath());
			}
		}
		String extractor = "Pablo-Sub-sequences";
		//String[] relationships = new String[]{"Indictment-Arrest-Trial"};
		String[] relationships = new String[]{args[2]};
		//Parameters to Evaluate Query Methods
		//Number of queries per method
		int numberOfQueries = 100;
		//Number of retrieved docs per query
		int numberOfRetrievedDocsInSample = 10;
		//beta for scores
		double beta = 0.5;
		
		int split = Integer.parseInt(args[3]);;
		//Number of queries for the initial sample
		int numQueries = 50;
		int sampleSize = 2000;

		String sampling = args[4];
		//String sampling = "Explicit";
		//String sampling = "Query";
		
		//Scenario to use
		boolean isFullAccessScenario = Boolean.parseBoolean(args[5]);
		//boolean isFullAccessScenario = true;
		int docsPerQueryLimit = -1;
		String scenario = "full";
		if(!isFullAccessScenario){
			//If this value is -1 we do not care about the docs per query... There are scenarios in which
			//this cannot happen
			//docsPerQueryLimit = -1;
			docsPerQueryLimit = Integer.parseInt(args[6]);
			scenario = "search";
			if(docsPerQueryLimit!=-1){
				scenario=scenario+docsPerQueryLimit;
			}
		}


		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationRealIndex"));
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationNewIndex"));
		Directory directory = new SimpleFSDirectory(new File(args[1]));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		//Set<String> collectionFixed = indexCollection(path,subPaths,conn);
		Set<String> collectionFixed = conn.getAllFiles();

		for(String relationship : relationships){
			String resultsPath = "results" + relationship;
			Set<String> collection=new MemoryEfficientHashSet<String>(collectionFixed);
			Set<String> knownDocuments = new MemoryEfficientHashSet<String>();
			String[] fieldsVector = new String[]{NYTDocumentWithFields.ALL_FIELD};
			QueryParser qp = new MultiFieldQueryParser(
	                Version.LUCENE_41, 
	                fieldsVector,
	                new StandardAnalyzer(Version.LUCENE_41));
			String featSel = "ChiSquaredWithYatesCorrectionAttributeEval"; //InfoGainAttributeEval
			String extr;
			String initialQueriesPath;
			if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
				extr = relationship;
				initialQueriesPath = "QUERIES/" + relationship + "/" + true + "/SelectedAttributes/" + relationship + "-" + split;
			}else{
				extr = "SSK-"+relationship+"-SF-"+(relationship.equals("ManMadeDisaster")? "HMM":"CRF")+"-relationWords_Ranker_";
				initialQueriesPath = "QUERIES/" + relationship + "/" + true + "/SelectedAttributes/" + extr + featSel + "_"+split+"_5000.words";
			}
			List<Query> initialQueries = loadQueries(qp, initialQueriesPath,numQueries);

			System.out.println("Initializing the Query Generation Methods");
			Set<String> fields = new HashSet<String>();
			for(String field : fieldsVector){
				fields.add(field);
			}
			List<QueryGenerationMethod> qgm = new ArrayList<QueryGenerationMethod>();
			//FeatureExtractor feB = new AllFieldsTermFrequencyFeatureExtractor(conn, fields, false,false,true);
			//FeaturesRanker<String> frB = new ValueBasedFeatureRanker<String>(true);
			//ScoresCombinator<String> fcB = new SumScoreCombinator<String>();
			//qgm.add(new FeatureExtractorBasedQueryGenerationMethod(feB, frB, fcB, qp));			
			FeatureExtractor feB = new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.ALL_FIELD);
			FeaturesRanker<String> frB = new ValueBasedFeatureRanker<String>(true);
			ScoresCombinator<String> fcB = new SumScoreCombinator<String>();
			qgm.add(new FeatureExtractorBasedQueryGenerationMethod(feB, frB, fcB, qp));
			//FeatureExtractor feL = new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.LEAD_FIELD);
			//FeaturesRanker<String> frL = new ValueBasedFeatureRanker<String>(true);
			//ScoresCombinator<String> fcL = new SumScoreCombinator<String>();
			//qgm.add(new FeatureExtractorBasedQueryGenerationMethod(feL, frL, fcL, qp));
			//FeatureExtractor feT = new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.TITLE_FIELD);
			//FeaturesRanker<String> frT = new ValueBasedFeatureRanker<String>(true);
			//ScoresCombinator<String> fcT = new SumScoreCombinator<String>();
			//qgm.add(new FeatureExtractorBasedQueryGenerationMethod(feT, frT, fcT, qp));
			qgm.add(new AttributeValuesQGM(qp));
			qgm.add(new SignificantPhrasesQGM(pathDictionary, qp, 2, 10));
			qgm.add(new FactTypeNameQGM(qp, relationship));
			qgm.add(new InitialQueriesQGM(qp, initialQueriesPath));
			//qgm.add(new LexicoSyntacticFactTypeQGM(qp));
			if(isFullAccessScenario){
				qgm.add(new AllDocumentsQueryQGM(qp));
			}

			//XXX I am not sure if this is the right features I should be looking at, or the queries that are generated with the QGM.
			
			FeaturesCoordinator coordinator = new FeaturesCoordinator();
			coordinator.addFeatureExtractor(feB);
			for(Query q : initialQueries){
				coordinator.addFeatureExtractor(new MatchesQueryFeatureExtractor(conn, q));
			}			
			
			System.out.println("Initiating IE programs");
			AdditiveFileSystemWrapping extractWrapper = new AdditiveFileSystemWrapping();
			for(String subPath : subPaths){
				if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationship + ".data");
				}else{
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + ".data");
				}
			}

			System.out.println("Obtaining initial sample (to use Pablo's sampling techniques)");
			SamplingTechnique sampler;
			if(sampling.equals("Explicit")){
				String[] documents = new String[2];
				documents[0] = String.format(folderDesign, split*2-1);
				documents[1] = String.format(folderDesign, split*2);
				System.out.println(Arrays.toString(documents));
				
				sampler = new ExplicitSamplingTechnique(path, documents);
			}else if(sampling.equals("Query")){			
				sampler = new InitialQuerySamplingTechnique(conn, qp, initialQueriesPath, numberOfRetrievedDocsInSample,sampleSize);
			}else if (sampling.equals("Smart")){
				sampler = new CyclicInitialQuerySamplingTechnique(conn, qp, initialQueriesPath, numberOfRetrievedDocsInSample,numQueries,sampleSize);
			}else{
				throw new UnsupportedOperationException("No sampling parameter: '" + sampling + "'");
			}
			List<String> sample = sampler.getSample();
			System.out.println("\tThe sample contains " + sample.size() + " documents.");
			knownDocuments.addAll(sample);
			System.out.println("\tThe collection without the sample contains " + (collection.size()-sample.size()) + " documents.");

			System.out.println("Extracting information from the sample");
			List<String> relevant = new ArrayList<String>();
			List<String> nonRelevant = new ArrayList<String>();
			Map<String,List<Tuple>> tuples = new MemoryEfficientHashMap<String, List<Tuple>>();
			for(String doc : sample){
				List<Tuple> tup = extractWrapper.getTuplesDocument(doc);
				if (tup.isEmpty()){
					nonRelevant.add(doc);
				}else{
					relevant.add(doc);
					tuples.put(doc, tup);
				}
			}
			System.out.println("\tThere are " +relevant.size() +" relevant documents in the sample.");
			System.out.println("\tThere are " +(nonRelevant.size()) +" non relevant documents in the sample.");

			Map<String,Integer> relevance = new MemoryEfficientHashMap<String, Integer>();

			int numDocs = (collection.size()-sample.size());

			for(String doc : collection){
				
				if(!sample.contains(doc)){
					int num = extractWrapper.getNumTuplesDocument(doc);
					if(num!=0){
						relevance.put(doc, num);
					}
				}
			}

			
			System.out.println("Initializing FactCrawl");
			RFACT factCrawl = new RFACT(extractWrapper, conn);

			//The adaptive process goes on here
			
			System.out.println("Generate Queries With Feature Selection Methods");
			QMDGraph graph = factCrawl.evaluateQueryGenerationMethods(qgm,numberOfQueries,numberOfRetrievedDocsInSample, docsPerQueryLimit, relevant, nonRelevant, tuples,new HashSet<String>(sample),null);

			System.out.println("Extract Crawled Documents");
			QMDScores scores = new FactCrawlQMDScores(beta,graph);
			factCrawl.extractDocuments(graph,scores);

			System.out.println("Preparing update decider");
			UpdateDecision updateDecision = null;
			UpdatePrediction updatePrediction = null;
			if(updateMethod.equals("Window")){
				updateDecision = new ExactWindowUpdateDecision(graph.getAllDocs().size()/50);
			}else if(updateMethod.equals("Shifting")){
				updatePrediction = new FeatureShiftOnline(sample, relevant, conn, coordinator, 1000/*instances to look*/);
			}else if(updateMethod.equals("FeatureRank")){
				updateDecision = new FeatureRankOnline(new ArrayList<String>(sample), new ArrayList<String>(relevant), coordinator, 0.05, 200);
			}else{
				throw new UnsupportedOperationException("No update parameter: '" + sampling + "'");
			}	
			
			
			System.out.println("Re-rank Fixed");
			ExtractionStrategy strategyf = new RegenerationQueriesStrategy(relevance, conn,graph,scores, updateDecision, updatePrediction, new StaticExtractionStrategy(conn,graph,scores),factCrawl,qgm,extractWrapper,numberOfQueries,numberOfRetrievedDocsInSample, docsPerQueryLimit,beta,sampleSize, new HashSet<String>(sample),measurer); // Regular FactCrawl
			List<String> sortedDocumentsf = factCrawl.sortDocuments(graph.getAllDocs(),strategyf);

			/* I don't think adaptive makes sense for this scenario, since we are updating the scores when we do the new queries. If it does, we need to re-implement it because of the score that updates all the time.
			
			System.out.println("Re-rank Adaptive");
			List<Integer> allDocs = graph.getAllDocs();
			System.out.println(allDocs.size() + " documents to process!");
			int updateAfter = allDocs.size()/50;
			ExtractionStrategy strategy = new RegenerationQueriesStrategy(relevance, conn,graph,scores, updateDecision, updatePrediction, new AdaptiveExtractionStrategy(conn,graph,scores,updateAfter,extractWrapper,relevance),factCrawl,qgm,extractWrapper,numberOfQueries,numberOfRetrievedDocsInSample, docsPerQueryLimit,beta,sampleSize,new HashSet<String>(sample)); //Adaptive FactCrawl
			List<String> sortedDocuments = factCrawl.sortDocuments(graph.getAllDocs(), strategy);

			*/
						
			//and the adaptive process finishes here
			
			/*System.out.println("Generating the Excel File with " + numDocs + " documents");
			gen.addRankingCurve("Random Ranking", new BaselineCurve(numDocs));
			gen.addRankingCurve("Perfect Ranking", new PerfectCurve(numDocs, relevance));
			gen.addRankingCurve("FactCrawlF", new SortedCurve(numDocs, sortedDocumentsf, relevance));
			gen.addRankingCurve("FactCrawlA", new SortedCurve(numDocs, sortedDocuments, relevance));
			gen.generateExcel("testFactCrawl" + relationship + ".xls");*/

			File folder = new File("resultsRank");
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/" + scenario);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/" + scenario +"/" + sampling);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/" + scenario +"/" + sampling + "/factcrawl/");
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/" + scenario +"/" + sampling + "/factcrawl/" + relationship);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/" + scenario +"/" + sampling + "/factcrawl/" + relationship + "/" + split);
			if(!folder.exists()){
				folder.mkdir();
			}
			
			System.err.println(sortedDocumentsf.size());
			
			SerializationHelper.write("resultsRank/" + scenario +"/" + sampling + "/factcrawl/" + relationship + "/" + split + "/queriesReg.data", new SortedCurve(numDocs, sortedDocumentsf, relevance));
			SerializationHelper.write("resultsRank/" + scenario +"/" + sampling + "/factcrawl/" + relationship + "/" + split + "/times.Times", measurer.getCheckPoints());
//			SerializationHelper.write("resultsRank/" + scenario +"/" + sampling + "/factcrawl/" + relationship + "/" + split + "/adaptive.data", new SortedCurve(numDocs, sortedDocuments, relevance));
		}
	}


	private static List<Query> loadQueries(QueryParser qp, String queryFile, int numQueries) throws ParseException, IOException {
		InitialWordLoader iwl = new FromWeightedFileInitialWordLoader(qp,queryFile);
		List<Query> words = iwl.getInitialQueries().subList(0, numQueries);
		return words;
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
