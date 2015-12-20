package edu.columbia.cs.ltrie;

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
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import pt.utl.ist.online.learning.engines.ElasticNetLinearPegasosEngine;
import pt.utl.ist.online.learning.engines.LinearOnlineEngine;
import pt.utl.ist.online.learning.utils.MemoryEfficientHashMap;
import pt.utl.ist.online.learning.utils.MemoryEfficientHashSet;
import pt.utl.ist.online.learning.utils.Pair;
import pt.utl.ist.online.learning.utils.Statistics;
import pt.utl.ist.online.learning.utils.TimeMeasurer;
import pt.utl.ist.online.learning.utils.UpdateStatistics;

import com.nytlabs.corpus.NYTCorpusDocument;
import com.nytlabs.corpus.NYTCorpusDocumentParser;

import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.FromWeightedFileInitialWordLoader;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.InitialWordLoader;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.impl.FromFileInitialWordLoader;
import edu.columbia.cs.ltrie.datamodel.DocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.excel.curves.BaselineCurve;
import edu.columbia.cs.ltrie.excel.curves.PerfectCurve;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.AdditiveFileSystemWrapping;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.CompressedAdditiveFileSystemWrapping;
import edu.columbia.cs.ltrie.features.AllFieldsTermFrequencyFeatureExtractor;
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
import edu.columbia.cs.ltrie.updates.ExactWindowUpdateDecision;
import edu.columbia.cs.ltrie.updates.FeatureRankOnline;
import edu.columbia.cs.ltrie.updates.FeatureShiftOnline;
import edu.columbia.cs.ltrie.updates.FeatureShifting;
import edu.columbia.cs.ltrie.updates.ModelSimilarityUpdateDecision;
import edu.columbia.cs.ltrie.updates.UpdateDecision;
import edu.columbia.cs.ltrie.updates.UpdatePrediction;
import edu.columbia.cs.ltrie.utils.SerializationHelper;

public class AdaptiveScanWithSVMDocSent {
	
	private static final String DOCUMENT_FIELD = "ORIGINAL_DOCUMENT";

	
	public static void main(String[] args) throws Exception {
		
		String suffix = "-sentence.data";//".data";
		
		TimeMeasurer measurer = new TimeMeasurer();
		//Declarations
		String path = args[0];
		//String path = "/home/goncalo/NYTValidationSplit/";
		File pathF = new File(path);
		int numPaths=pathF.list().length;
		String[] subPaths = new String[numPaths];
		String folderDesign = "%0" + String.valueOf(numPaths).length() + "d";
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format(folderDesign, i);
		}
		String extractor = "Pablo-Sub-sequences";
		String[] relationships = new String[]{args[3]};
		//String[] relationships = new String[]{"Indictment-Arrest-Trial"};

		//String[] relationships = new String[]{"VotingResult"};
		int docsPerQuerySample = 10;
		int numQueries = 50;
		int sampleSize = 2000;
		int split = Integer.parseInt(args[6]);
		
		String updateMethod = args[4];
		//String updateMethod = "Window";
		//String updateMethod = "Inner";
		//String updateMethod = "FeatureRank";
		
		String sampling = args[5];
		//String sampling = "Explicit";
		//String sampling = "Query";

		int maxNumDocs = Integer.parseInt(args[7]);
		
		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		//analyzer = new ShingleAnalyzerWrapper(analyzer, 2, 2);
		//Directory directory = new RAMDirectory();
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationRealIndex"));
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationNewIndex"));
		Directory directory = new SimpleFSDirectory(new File(args[1]));
		//Directory directory = new MMapDirectory(new File(args[1]));
		//Directory directory = new SimpleFSDirectory(new File(args[1]), NoLockFactory.getNoLockFactory());
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		//Set<String> collectionFixed = indexCollection(path,subPaths,conn);
		
		Directory directory_sent = new SimpleFSDirectory(new File(args[2]));
		IndexConnector conn_sent = new IndexConnector(analyzer, directory_sent, "");

		
		Set<String> collection = conn.getAllFiles();
//		Set<String> collectionFixed = conn.getAllFiles();
		
		for(String relationship : relationships){
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

			System.out.println(initialQueries.size());
			
			//Set<String> collection=new HashSet<String>(collectionFixed);
			String resultsPath = "results" + relationship;
			System.out.println("Initiating IE programs");
			CompressedAdditiveFileSystemWrapping extractWrapper = new CompressedAdditiveFileSystemWrapping();
			for(String subPath : subPaths){
				if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationship + suffix);
				}else{
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + suffix);
				}
			}
			
			List<List<Tuple>> allTuples = new ArrayList<List<Tuple>>();
			
			System.out.println("Preparing feature extractors");
			FeaturesCoordinator coordinator = new FeaturesCoordinator();
			Set<String> fields = new HashSet<String>();
			for(String field : fieldsVector){
				fields.add(field);
			}
			//coordinator.addFeatureExtractor(new AllFieldsTermFrequencyFeatureExtractor(conn, fields, false,false,true));
			coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn_sent, NYTDocumentWithFields.ALL_FIELD,false,false,true));
			//coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.LEAD_FIELD,false,false,true));
			//coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.BODY_FIELD,false,false,true));
			for(Query q : initialQueries){
				Set<Term> terms = new HashSet<Term>();
				q.extractTerms(terms);
				if(terms.size()>1){
					coordinator.addFeatureExtractor(new MatchesQueryFeatureExtractor(conn_sent, q));
				}
			}

			System.out.println("Obtaining initial sample (to use Pablo's sampling techniques)");
			
			SamplingTechnique sampler;
			if(sampling.equals("Explicit")){
				String[] documents = new String[2];
				documents[0] = String.format(folderDesign, split*2-1);
				documents[1] = String.format(folderDesign, split*2);
				
				sampler = new ExplicitSamplingTechnique(path, documents);
			}else if(sampling.equals("Query")){			
				sampler = new InitialQuerySamplingTechnique(conn, qp, initialQueriesPath, docsPerQuerySample,sampleSize);
			}else if (sampling.equals("Smart")){
				sampler = new CyclicInitialQuerySamplingTechnique(conn, qp, initialQueriesPath, docsPerQuerySample,numQueries,sampleSize);
			}else{
				throw new UnsupportedOperationException("No sampling parameter: '" + sampling + "'");
			}
			List<String> sample = sampler.getSample();
			System.out.println("\tThe sample contains " + sample.size() + " documents.");
			collection.removeAll(sample);
			System.out.println("\tThe collection without the sample contains " + collection.size() + " documents.");
			
			System.out.println("Extracting information from the sample");
			List<String> relevantSents = new ArrayList<String>();
			List<String> sents = new ArrayList<String>();
			
			BooleanQuery containQuery = new BooleanQuery();
			BooleanQuery.setMaxClauseCount(Math.max(1, sample.size()));
			
			for(String doc : sample){
				
				containQuery.add( new TermQuery(new Term(DOCUMENT_FIELD, doc)), Occur.SHOULD);
			}
			
			List<Integer> local_sents = conn_sent.search(containQuery);
			
			System.out.format("Docs: %d matched %d sentneces\n", sample.size(),local_sents.size());
			
			for (Integer local_sent : local_sents) {
				
				String local_sent_str = conn_sent.getPath(local_sent);
				
				List<Tuple> tuples = extractWrapper.getTuplesDocument(local_sent_str);
				if(tuples.size()!=0){
					relevantSents.add(local_sent_str);
					allTuples.add(tuples);
				}
				sents.add(local_sent_str);
				addTupleFeatures(qp, tuples);
				
			}
			
			System.out.println("\tThere are " +relevantSents.size() +" relevant sentences in the sample.");
			System.out.println("\tThere are " +(sents.size()-relevantSents.size()) +" non relevant sentences in the sample.");
			
			System.out.println("Initial training of the ranking model");
			LinearOnlineEngine<Long> engine = new ElasticNetLinearPegasosEngine<Long>(0.1,0.99, 1, false);
			//LinearOnlineEngine<Long> engine = new LinearPegasosEngine<Long>(0.1, 1.0,false);
			OnlineRankingModel model = new OnlineRankingModel(coordinator, sents, relevantSents, engine, 10000);

			System.out.println(getTopKQueries(model,coordinator,10));
			
			
			System.out.println("Performing initial ranking");
			//Map<String,Double> scoresCollection = model.getScores(collection);
			Map<Query,Double> queryScores = model.getQueryScores();
			Map<String,Float> scoresCollection = conn.getScores(queryScores, new SimpleBooleanSimilarity(), collection);
			List<String> initialRankingDocument = sortCollection(scoresCollection);
			List<String> rankedCollection = new ArrayList<String>(initialRankingDocument);

			
			
			containQuery = new BooleanQuery();
			BooleanQuery.setMaxClauseCount(Math.max(1, maxNumDocs));
			
			Set<String> knownDocuments = new HashSet<String>();
			
			for (int i = 0; i < maxNumDocs && !rankedCollection.isEmpty(); i++) {
				
				String doc = rankedCollection.remove(0);
				collection.remove(doc);
				knownDocuments.add(doc);
				containQuery.add( new TermQuery(new Term(DOCUMENT_FIELD, doc)), Occur.SHOULD);
			
			}
			
			local_sents = conn_sent.search(containQuery);
			Set<String> sentsToRank = new HashSet<String>(local_sents.size());
			for (Integer local_sent : local_sents) {
				
				sentsToRank.add(conn_sent.getPath(local_sent));
			
			}
			
			
			Map<String,Float> scoresSentences = conn_sent.getScores(queryScores, new SimpleBooleanSimilarity(), sentsToRank, knownDocuments);
			List<String> initialRanking = sortCollection(scoresSentences);
			List<String> rankedSentences = new ArrayList<String>(initialRanking);
						
			
			
			Map<Query, Double> previousScores = queryScores;
			
			System.out.println("Extracting information");
			Map<String,Integer> relevance = new HashMap<String, Integer>();
			List<String> adaptiveRanking = new ArrayList<String>();
			
			int numUpdates=0;

			System.out.println("Preparing update decider");
			UpdateDecision updateDecision = null;
			UpdatePrediction updatePrediction = null;
			if(updateMethod.equals("Window")){
				int collectionSize=rankedCollection.size();
				System.out.println("It will update 50 times during the process. I.e., it will process pretty much all sentences of all documents before updating.");
				updateDecision = new ExactWindowUpdateDecision(collectionSize/50);
			}else if(updateMethod.equals("Shifting")){
				updatePrediction = new FeatureShiftOnline(sents, relevantSents, conn_sent, coordinator, 1000/*instances to look*/);
			}else if(updateMethod.equals("FeatureRank")){
				updateDecision = new FeatureRankOnline(new ArrayList<String>(sents), new ArrayList<String>(relevantSents), coordinator, 0.05, 200);
			}else if(updateMethod.equals("ModelSimilarity")){
				updateDecision = new ModelSimilarityUpdateDecision(model, 5, 0.1);
			}else{
				throw new UnsupportedOperationException("No update parameter: '" + sampling + "'");
			}
			//UpdateDecision updateDecision = new ExactWindowUpdateDecision(1000);
			//UpdateDecision updateDecision = new AveragePrecisionBasedUpdateDecision(relevantDocs.size(),docs.size());
			//UpdateDecision updateDecision = new AveragePrecisionBasedUpdateDecision(0.05);
			//UpdateDecision updateDecision = new InnerProductBasedUpdateDecision(model, sample, relevantDocs);
			//UpdateDecision updateDecision = new FeatureShifting(new ArrayList<String>(relevantDocs), 0.005f, 6, 0.5, conn, 1000);
			//UpdateDecision updateDecision = new FeatureRankComparison(conn, 500, 1000d, new ArrayList<String>(docs), new ArrayList<String>(relevantDocs), 1000,FeatureRankComparison.SPEARMANS_FOOTRULE);
			
			System.out.println("Initial feature space size: " + coordinator.getCurrentNumberFeatures());
			
			List<Integer> updates = new ArrayList<Integer>();
			List<Statistics> updateStatistics = new ArrayList<Statistics>();
			
			int rel=0;
			int nRel=0;
			
			List<String> currentBatchSents = new ArrayList<String>();
			List<String> currentBatchRelSents = new ArrayList<String>();
			
			int sentencesSize = rankedSentences.size();
			
			Set<String> knownSents = new HashSet<String>();
			
			for(int i=0; i<sentencesSize; i++){
				
				String sent = rankedSentences.get(i);
				if (!knownSents.add(sent))
					System.out.println("Duplicated sentence:" + sent);
				sentsToRank.remove(sent);
				adaptiveRanking.add(sent);
				currentBatchSents.add(sent);
				List<Tuple> tuples = extractWrapper.getTuplesDocument(sent);
				int num = tuples.size();
				if(num!=0){
					currentBatchRelSents.add(sent);
					relevance.put(sent, num);
					rel++;
					allTuples.add(tuples);
				}else{
					nRel++;
				}
				measurer.addCheckPoint();

				addTupleFeatures(qp, tuples);

				if((updateDecision != null && updateDecision.doUpdate(currentBatchSents, currentBatchRelSents)) || 
						(updatePrediction != null && updatePrediction.predictUpdate(rankedCollection,i))){
					updates.add(adaptiveRanking.size());
					submitTopTuples(conn_sent, coordinator, 100);
					System.out.println("\tUpdating ranking model " + (++numUpdates));
					Map<Long, Double> oldVector = new HashMap<Long, Double>();
					for (Entry<Long, Double> entry : model.getWeightVector().entrySet()) {
						oldVector.put(entry.getKey(), entry.getValue());
					}
					model.updateModel(new HashSet<String>(currentBatchSents), new HashSet<String>(currentBatchRelSents), currentBatchSents.size());
					updateStatistics.add(new UpdateStatistics(oldVector, model.getWeightVector()));
					System.out.println("New model uses " + model.getWeightVector().size() + " features.");
					System.out.println("Top features: ");
					System.out.println(getTopKQueries(model,coordinator,10));
					System.out.println("\tRe-ranking");
					//Map<String,Double> newScoresCollection = model.getScores(collection);
					Map<Query,Double> newQueryScores = model.getQueryScores();
					Map<String,Float> newScoresCollection = conn.getScores(newQueryScores, new SimpleBooleanSimilarity(), collection);
					rankedCollection = sortCollection(newScoresCollection);
					System.out.println("\tFinished Ranking - Back to extraction");
					System.out.println("\t" + rankedCollection.size() + " documents to go.");
					
					BooleanQuery.setMaxClauseCount(Math.max(1, knownDocuments.size() + maxNumDocs));
					
					for (int nd = 0; nd < maxNumDocs && !rankedCollection.isEmpty(); nd++) {
						
						String doc = rankedCollection.remove(0);
						collection.remove(doc);
						knownDocuments.add(doc);
						containQuery.add( new TermQuery(new Term(DOCUMENT_FIELD, doc)), Occur.SHOULD);
					
					}
					
					local_sents = conn_sent.search(containQuery);
					for (Integer local_sent : local_sents) {
						
						String sente = conn_sent.getPath(local_sent);
						if (!knownSents.contains(sente))
							sentsToRank.add(sente);
					
					}

					Map<String,Float> newscoresSentences = conn_sent.getScores(newQueryScores, new SimpleBooleanSimilarity(), sentsToRank, knownDocuments);
					rankedSentences = sortCollection(newscoresSentences);

					previousScores = newQueryScores;
					
					i=-1;
					sentencesSize=rankedSentences.size();
					

					if (updateDecision != null)
						updateDecision.reset();
					if (updatePrediction != null){
						updatePrediction.performUpdate(currentBatchSents, currentBatchRelSents);
					}
					
					currentBatchSents = new ArrayList<String>();
					currentBatchRelSents = new ArrayList<String>();
				} 
				
				if (i == sentencesSize-1 && !collection.isEmpty()){ //check that I did not run out of sentences to process
				
					System.out.println("Run out of sentences...");
					
					knownDocuments.clear(); //Can clear because we exhausted all documents
					
					containQuery = new BooleanQuery();
					BooleanQuery.setMaxClauseCount(Math.max(1, maxNumDocs));
					
					for (int nd = 0; nd < maxNumDocs && !rankedCollection.isEmpty(); nd++) { //I need th next batch of documents.
						
						String doc = rankedCollection.remove(0);
						collection.remove(doc);
						knownDocuments.add(doc);
						containQuery.add( new TermQuery(new Term(DOCUMENT_FIELD, doc)), Occur.SHOULD);
					
					}
					
					local_sents = conn_sent.search(containQuery);
					for (Integer local_sent : local_sents) {
						String sente = conn_sent.getPath(local_sent);
						if (!knownSents.contains(sente))
							sentsToRank.add(sente);
					}
					
					Map<String,Float> newscoresSentences = conn_sent.getScores(previousScores, new SimpleBooleanSimilarity(), sentsToRank, knownDocuments);
					rankedSentences = sortCollection(newscoresSentences);

					i=-1;
					sentencesSize=rankedSentences.size();
					
				}
				
				
				int currentNumberOfProcessedSent = adaptiveRanking.size();
				if(currentNumberOfProcessedSent%1000==0){
					System.out.println("Processed " + currentNumberOfProcessedSent + " sentences (" + (rel*100.0/(rel+nRel))+ "% precision)");
				}
			}
			
			System.out.println("Final feature space size: " + coordinator.getCurrentNumberFeatures());

			System.out.println("Plotting results");
			//ExcelGenerator gen = new ExcelGenerator();
			int numSentences = adaptiveRanking.size();
			/*gen.addRankingCurve("RandomRanking", new BaselineCurve(numDocs));
			gen.addRankingCurve("PerfectRanking", new PerfectCurve(numDocs, relevance));
			gen.addRankingCurve("InitialRanking", new SortedCurve(numDocs, initialRanking, relevance));
			gen.addRankingCurve("AdaptiveRanking", new SortedCurve(numDocs, adaptiveRanking, relevance));
			//gen.generateExcel("test" + relationship + "Adaptive.xls");
			gen.generateR("test_" + relationship + "Adaptive",1000);*/
			
			File folder = new File("resultsRank");
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/fullds");
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/fullds/" + sampling);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/fullds/" + sampling + "/"+ updateMethod);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/fullds/" + sampling + "/"+ updateMethod + "/" + relationship);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/fullds/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split);
			if(!folder.exists()){
				folder.mkdir();
			}
			SerializationHelper.write("resultsRank/fullds/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/baseline.data", new BaselineCurve(numSentences));
			SerializationHelper.write("resultsRank/fullds/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/perfect.data", new PerfectCurve(numSentences, relevance));
			SerializationHelper.write("resultsRank/fullds/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/initialRankSVM.data", new SortedCurve(numSentences, initialRanking, relevance));
			SerializationHelper.write("resultsRank/fullds/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVM.data", new SortedCurve(numSentences, adaptiveRanking, relevance));
			SerializationHelper.write("resultsRank/fullds/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVMUpdates.updates", updates);
			SerializationHelper.write("resultsRank/fullds/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVMUpdates.updates.statistics", updateStatistics);
			SerializationHelper.write("resultsRank/fullds/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVMTimes.times", measurer.getCheckPoints());
			SerializationHelper.write("resultsRank/fullds/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVMtuples.tuples", allTuples);
			if(updateDecision!=null){
				System.out.println(updateDecision.report());
			}
			if(updatePrediction!=null){
				System.out.println(updatePrediction.report());
			}
		}
	}

	//private static Map<String,Integer> previouslyUsedValues = new MemoryEfficientHashSet<String>();
	private static Map<Query,Integer> previouslySeenValues = new MemoryEfficientHashMap<Query,Integer>();

	private static void addTupleFeatures(QueryParser p, List<Tuple> tuples) throws ParseException, IOException {
		int tuplesSize= tuples.size();
		Set<String> seenInThisDocument = new HashSet<String>();
		for (int i = 0; i < tuplesSize; i++) {
			Tuple t = tuples.get(i);

			Set<String> fields = t.getFieldNames();
			for (String field : fields) {
				String val = t.getData(field).getValue();
				String quer = "+\"" + QueryParser.escape(val) + "\"";
				Query q = p.parse(quer);
				Set<Term> terms = new HashSet<Term>();
				q.extractTerms(terms);
				if(terms.size()>1){
					String qToString = q.toString();
					
					if(!seenInThisDocument.contains(qToString)){
						Integer freq = previouslySeenValues.get(qToString);
						if(freq==null){
							freq=0;
						}
						previouslySeenValues.put(q, freq+1);
					}
					seenInThisDocument.add(qToString);
					
					/*if(previouslySeenValues.contains(qToString)  && !seenInThisDocument.contains(qToString) && !previouslyUsedValues.contains(qToString)){
						coord.addFeatureExtractor(new MatchesQueryFeatureExtractor(conn, q));
						previouslyUsedValues.add(qToString);
						//System.out.println((++numQueries) + "/" + previouslySeenValues.size() + " " + qToString);
					}
					previouslySeenValues.add(qToString);
					seenInThisDocument.add(qToString);*/
				}
			}
		}
	}
	
	private static Set<String> previouslyUsedValues = new MemoryEfficientHashSet<String>();
	
	private static void submitTopTuples(IndexConnector conn, FeaturesCoordinator coord, int numNewQueries)
	throws IOException{
		List<Entry<Query,Integer>> frequencies = new ArrayList<Entry<Query,Integer>>(previouslySeenValues.entrySet());
		Collections.sort(frequencies, new Comparator<Entry<Query,Integer>>(){

			@Override
			public int compare(Entry<Query, Integer> arg0,
					Entry<Query, Integer> arg1) {
				return arg1.getValue()-arg0.getValue();
			}
		});
		
		int i=0;
		int submittedQueries=0;
		while(submittedQueries<numNewQueries && i<frequencies.size()){
			Query q = frequencies.get(i).getKey();
			String qToString = q.toString();
			if(!previouslyUsedValues.contains(qToString)){
				coord.addFeatureExtractor(new MatchesQueryFeatureExtractor(conn, q));
				previouslyUsedValues.add(qToString);
				submittedQueries++;
			}
			
			i++;
		}
	}

	private static List<Query> loadQueries(QueryParser qp, String queryFile, int numQueries) throws ParseException, IOException {
		InitialWordLoader iwl = new FromWeightedFileInitialWordLoader(qp,queryFile);
		List<Query> words = iwl.getInitialQueries().subList(0, numQueries);
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
	
	private static List<Pair<Pair<String,String>,Double>> getTopKQueries(OnlineRankingModel model,
			FeaturesCoordinator coordinator, int k) {
		Map<Long,Double> weightVector = model.getWeightVector();
		System.out.println(weightVector.size() + " features.");
		double rho = model.getRho();
		Map<Pair<String,String>, Double> termWeights = new HashMap<Pair<String,String>, Double>();
		for(Entry<Long,Double> entry : weightVector.entrySet()){
			Pair<String,String> term = coordinator.getTerm(entry.getKey());
			if(term!=null && entry.getValue()>rho){
				termWeights.put(term, entry.getValue());
			}
		}

		final Map<Pair<String,String>,Double> scores = termWeights;
		List<Pair<String,String>> queries = new ArrayList<Pair<String,String>>(termWeights.keySet());
		Collections.sort(queries, new Comparator<Pair<String,String>>() {
			@Override
			public int compare(Pair<String, String> o1, Pair<String, String> o2) {
				return (int) Math.signum(scores.get(o2)-scores.get(o1));
			}
		});

		List<Pair<Pair<String,String>,Double>> results = new ArrayList<Pair<Pair<String,String>,Double>>();
		int queriesSize=queries.size();
		for(int i=0; i<Math.min(queriesSize, k); i++){
			Pair<String,String> query = queries.get(i);
			results.add(new Pair<Pair<String,String>, Double>(query, scores.get(query)));
		}

		return results;
	}
}
