package edu.columbia.cs.ltrie;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import com.google.gdata.util.common.base.Pair;
import com.nytlabs.corpus.NYTCorpusDocument;
import com.nytlabs.corpus.NYTCorpusDocumentParser;

import edu.columbia.cs.ltrie.baseline.factcrawl.graph.QMDGraph;
import edu.columbia.cs.ltrie.baseline.factcrawl.graph.scores.FactCrawlQMDScores;
import edu.columbia.cs.ltrie.baseline.factcrawl.graph.scores.QMDScores;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.InitialWordLoader;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.impl.FromFileInitialWordLoader;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.QueryGenerationMethod;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.AttributeValuesQGM;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.FeatureExtractorBasedQueryGenerationMethod;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.SignificantPhrasesQGM;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.UserDefinedSeedFactsQGM;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.utils.FeaturesRanker;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.utils.ScoresCombinator;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.utils.impl.SumScoreCombinator;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.utils.impl.ValueBasedFeatureRanker;
import edu.columbia.cs.ltrie.baseline.factcrawl.strategy.ExtractionStrategy;
import edu.columbia.cs.ltrie.baseline.factcrawl.strategy.impl.AdaptiveExtractionStrategy;
import edu.columbia.cs.ltrie.baseline.factcrawl.strategy.impl.StaticExtractionStrategy;
import edu.columbia.cs.ltrie.baseline.factcrawl.utils.MapBasedComparator;
import edu.columbia.cs.ltrie.datamodel.DocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.excel.ExcelGenerator;
import edu.columbia.cs.ltrie.excel.curves.BaselineCurve;
import edu.columbia.cs.ltrie.excel.curves.PerfectCurve;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.extractor.ExtractionSystem;
import edu.columbia.cs.ltrie.extractor.impl.cimple.CIMPLEExtractionSystem;
import edu.columbia.cs.ltrie.extractor.impl.reel.REELRelationExtractionSystem;
import edu.columbia.cs.ltrie.extractor.wrapping.ExtractionWrapper;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.AdditiveFileSystemWrapping;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.DirectExtractionWrapping;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.FileSystemWrapping;
import edu.columbia.cs.ltrie.features.FeatureExtractor;
import edu.columbia.cs.ltrie.features.TermFrequencyFeatureExtractor;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class RFACT {

	private ExtractionWrapper extractionSystem;
	private IndexConnector index;

	public RFACT(ExtractionWrapper extractionSystem, IndexConnector index) {
		this.extractionSystem = extractionSystem;
		this.index = index;
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalArgumentException 
	 * @throws WriteException 
	 * @throws RowsExceededException 
	 */
	public static void main(String[] args) throws IOException, ParseException, IllegalArgumentException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, RowsExceededException, WriteException {
		Logger.getRootLogger().removeAllAppenders();

		//Declarations
		String path = "/home/goncalo/NYTValidationSplit/";
		int numPaths=10;
		String[] subPaths = new String[numPaths];
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format("%03d", i);
		}
		String extractor = "Pablo-Sub-sequences";
		String relationship = "ManMadeDisaster";
		int docsPerQuery = 100;

		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		//analyzer = new ShingleAnalyzerWrapper(analyzer, 2, 2);
		Directory directory = new RAMDirectory();
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		Set<String> collectionFixed = indexCollection(path,subPaths,conn);


		QueryParser qp = new QueryParser(Version.LUCENE_CURRENT, NYTDocumentWithFields.BODY_FIELD,  analyzer);
		List<Query> initialQueries = loadQueries(qp, "factCrawlFiles/initial" + relationship +".txt");
		
		//Query Generation Methods
		List<QueryGenerationMethod> qgm = new ArrayList<QueryGenerationMethod>();
		FeatureExtractor fe = new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.BODY_FIELD);
		FeaturesRanker<String> fr = new ValueBasedFeatureRanker<String>(true);
		ScoresCombinator<String> fc = new SumScoreCombinator<String>();
		qgm.add(new FeatureExtractorBasedQueryGenerationMethod(fe, fr, fc, qp));
		qgm.add(new AttributeValuesQGM(qp));
		//ngram size
		int ngram = 2;
		int minSupport = 1;
		//qgm.add(new SignificantPhrasesQGM(path, qp, ngram,minSupport));

		//Parameters to Evaluate Query Methods
		//Number of queries per method
		int numberOfQueries = 100;
		//Number of retrieved docs per query
		int numberOfRetrievedDocs = 100;

		//beta for scores
		double beta = 0.5;

		File fDir = new File(path);

		System.out.println("Load Document");
		NYTCorpusDocumentParser parser = new NYTCorpusDocumentParser();
		List<DocumentWithFields> documentsWithFields = new ArrayList<DocumentWithFields>();

		System.out.println("Load Extractor");

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

		FileInputStream fis = new FileInputStream("resultsOrgAff/PersonCareer.ser");
		ObjectInputStream ois = new ObjectInputStream(fis);
		Map<String,Integer> relevance = (Map<String,Integer>) ois.readObject();
		ois.close();



		//		ExtractionWrapper w = new FileSystemWrapping("resultsOrgAff/PersonCareer.ser");
		//		System.out.println("Initialize");
		//		
		//		Map<String,Integer> relevance = new HashMap<String, Integer>();
		int numDocs = fDir.listFiles().length;
		//		
		//		File[] files = fDir.listFiles();
		//		
		//		for (int ff = files.length-1; ff > 0 ; ff--) {
		//			
		//			File f = files[ff];
		//			
		//			String fileName = f.getAbsolutePath();//.getName();
		//			int numTuples = w.getNumTuplesDocument(fileName);
		//			if(numTuples!=0){
		//				
		//				relevance.put(fileName,numTuples);
		//			}
		//		}


		RFACT factCrawl = new RFACT(extractWrapper, conn);

		System.out.println("Load Initial Words");

		System.out.println("Build Sample");

		Map<String,List<Tuple>> tuples = new HashMap<String,List<Tuple>>();

		List<String> relevant = new ArrayList<String>();

		List<String> nonRelevant = new ArrayList<String>();

		factCrawl.buildSample(initialQueries, 1000, docsPerQuery,relevant,nonRelevant,tuples,qp);

		System.out.println("Generate Queries With Feature Selection Methods");

		QMDGraph graph = factCrawl.evaluateQueryGenerationMethods(qgm,numberOfQueries,numberOfRetrievedDocs, relevant, nonRelevant, tuples);

		System.out.println("Extract Crawled Documents");

		QMDScores scores = new FactCrawlQMDScores(beta,graph);

		factCrawl.extractDocuments(graph,scores);

		System.out.println("Re-rank");

		//If running QXtract, do not do this the next step. Anyway, if you do it, you have another chance later on.

		//For each processed document will return a list of tuples (even if it is empty) in order.
		//If you are running QXtract, omit the docScores parameter unless it's null. 
		//If you got here with docScores, you can perform one extractDocuments using docScores 
		//		as parameter (FactCrawl) and another one right away without docScores (QXtract)

		//List<Pair<Integer,List<Tuple>>> output = factCrawl.extractDocuments(graph.getAllDocs(), docScores); it's in "relevance" ...

		ExtractionStrategy strategyf = new StaticExtractionStrategy(conn,graph,scores); // Regular FactCrawl

		List<String> sortedDocumentsf = factCrawl.sortDocuments(graph.getAllDocs(),strategyf);

		int updateAfter = 10;

		ExtractionStrategy strategy = new AdaptiveExtractionStrategy(conn,graph,scores,updateAfter,extractWrapper,relevance); //Adaptive FactCrawl

		List<String> sortedDocuments = factCrawl.sortDocuments(graph.getAllDocs(), strategy);

		ExcelGenerator gen = new ExcelGenerator();
		gen.addRankingCurve("Random Ranking", new BaselineCurve(numDocs));
		gen.addRankingCurve("Perfect Ranking", new PerfectCurve(numDocs, relevance));
		gen.addRankingCurve("FactCrawlF", new SortedCurve(numDocs, sortedDocumentsf, relevance));
		gen.addRankingCurve("FactCrawlA", new SortedCurve(numDocs, sortedDocuments, relevance));
		gen.generateExcel("test" + relationship + "FactCrawlAdaptive.xls");

	}

	private static void print(List<Pair<Integer, List<Tuple>>> output) {
		int total = 0;
		for (int i = 0; i < output.size(); i++) {
			total += output.get(i).getSecond().size(); 
			System.out.println(i + " - " + output.get(i).getSecond().size() + " - " + total);
		}

	}

	private void buildSample(List<Query> words, int sampleSize, int docsPerQuery, List<String> relevant,
			List<String> nonRelevant, Map<String, List<Tuple>> tuples,QueryParser qp) throws IOException, ParseException {

		Set<Integer> processed = new HashSet<Integer>();

		List<Query> copy = new ArrayList<Query>(words);

		for (int i = 0; i < copy.size() && relevant.size() < sampleSize; i++) {

			System.out.println("Collected Sample: " + relevant.size());

			List<Integer> res = index.search(copy.get(i), docsPerQuery);

			for (int j = 0; j < res.size() && relevant.size() < sampleSize; j++) {

				if (processed.contains(res.get(j)))
					continue;

				String doc = index.getPath(res.get(j));


				List<Tuple> tup = extractionSystem.getTuplesDocument(doc);

				if (tup.isEmpty()){
					nonRelevant.add(doc);
				}else{
					relevant.add(doc);

					tuples.put(doc, tup);

					for (int k = 0; k < tup.size(); k++) {
						copy.add(generateQuery(tup.get(k),qp));
					}
				}

			}

		}

	}

	private Query generateQuery(Tuple tuple, QueryParser qp) throws ParseException {

		return qp.parse(UserDefinedSeedFactsQGM.getStringTuple(tuple));

	}

	private List<Pair<Integer,List<Tuple>>> extractDocuments(List<Integer> docs) throws IOException{
		return extractDocuments(docs, null);
	}

	private List<Pair<Integer,List<Tuple>>> extractDocuments(List<Integer> docs, Map<Integer, Double> docScores)  throws IOException {
		if (docScores != null && !docScores.isEmpty())
			Collections.sort(docs,new MapBasedComparator<Integer>(docScores,true));

		List<Pair<Integer,List<Tuple>>> ret = new ArrayList<Pair<Integer,List<Tuple>>>();

		for (int i = 0; i < docs.size(); i++) {

			String doc = index.getPath(docs.get(i));

			ret.add(new Pair<Integer,List<Tuple>>(docs.get(i),extractionSystem.getTuplesDocument(doc)));

		}

		return ret;

	}

	private List<String> sortDocuments(Set<Integer> docs,
			ExtractionStrategy strategy) throws IOException, ParseException {
		return strategy.sortDocuments(docs);
	}

	private void extractDocuments(QMDGraph graph, QMDScores scores)  throws IOException {

		for (Integer docId : graph) {

			String doc = index.getPath(docId);

			int numTup = extractionSystem.getNumTuplesDocument(doc);

			scores.addDocumentTuples(docId,numTup);

		}

	}

	private QMDGraph evaluateQueryGenerationMethods(
			List<QueryGenerationMethod> qgm, int numberOfQueries, int hitsPerQuery, List<String> relevant, List<String> nonRelevant, Map<String, List<Tuple>> tuples) throws IOException, ParseException {

		QMDGraph graph = new QMDGraph();

		for (int i = 0; i < qgm.size(); i++) {

			List<Query> topQueries = qgm.get(i).generateQueries(numberOfQueries,relevant, nonRelevant,tuples);

			for (int j = 0; j < topQueries.size(); j++) {

				List<Integer> topDocs = index.search(topQueries.get(j));

				graph.store(topQueries.get(j),qgm.get(i),topDocs, hitsPerQuery);		

			}

		}

		return graph;		

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
	
	private static List<Query> loadQueries(QueryParser qp, String queryFile) throws ParseException, IOException {
		InitialWordLoader iwl = new FromFileInitialWordLoader(qp,queryFile);
		List<Query> words = iwl.getInitialQueries();
		return words;
	}

}
