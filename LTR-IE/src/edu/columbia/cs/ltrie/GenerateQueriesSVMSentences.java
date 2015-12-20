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

import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

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
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import pt.utl.ist.online.learning.engines.ElasticNetLinearPegasosConstantLearningRateEngine;
import pt.utl.ist.online.learning.engines.ElasticNetLinearPegasosEngine;
import pt.utl.ist.online.learning.engines.ElasticNetLinearPegasosSpecialEngine;
import pt.utl.ist.online.learning.engines.L1LinearPegasosEngine;
import pt.utl.ist.online.learning.engines.LinearOnlineEngine;
import pt.utl.ist.online.learning.engines.LinearPegasosEngine;
import pt.utl.ist.online.learning.engines.OnlineEngine;
import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.MemoryEfficientHashMap;
import pt.utl.ist.online.learning.utils.MemoryEfficientHashSet;
import pt.utl.ist.online.learning.utils.Pair;
import pt.utl.ist.online.learning.utils.TimeMeasurer;

import com.mysql.jdbc.UpdatableResultSet;
import com.nytlabs.corpus.NYTCorpusDocument;
import com.nytlabs.corpus.NYTCorpusDocumentParser;

import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.FromWeightedFileInitialWordLoader;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.InitialWordLoader;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.impl.FromFileInitialWordLoader;
import edu.columbia.cs.ltrie.datamodel.DocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.excel.ExcelGenerator;
import edu.columbia.cs.ltrie.excel.curves.BaselineCurve;
import edu.columbia.cs.ltrie.excel.curves.PerfectCurve;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.AdditiveFileSystemWrapping;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.CompressedAdditiveFileSystemWrapping;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.FileSystemWrapping;
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
import edu.columbia.cs.ltrie.updates.DisjunctionUpdateDecision;
import edu.columbia.cs.ltrie.updates.ExactWindowUpdateDecision;
import edu.columbia.cs.ltrie.updates.FeatureRankComparison;
import edu.columbia.cs.ltrie.updates.FeatureRankOnline;
import edu.columbia.cs.ltrie.updates.FeatureShiftOnline;
import edu.columbia.cs.ltrie.updates.FeatureShifting;
import edu.columbia.cs.ltrie.updates.ModelSimilarityUpdateDecision;
import edu.columbia.cs.ltrie.updates.UpdateDecision;
import edu.columbia.cs.ltrie.updates.UpdatePrediction;
import edu.columbia.cs.ltrie.utils.SerializationHelper;

public class GenerateQueriesSVMSentences {
	
	private static final String DOCUMENT_FIELD = "ORIGINAL_DOCUMENT";
	
	public static void main(String[] args) throws Exception {
		//Declarations
		TimeMeasurer measurer = new TimeMeasurer();

		String path = args[0];
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

		//If this value is -1 we do not care about the docs per query... There are scenarios in which
		//this cannot happen
		int docsPerQuery = Integer.parseInt(args[6]);
		int docsPerQuerySample = 10;
		int topQueriesInUpdate = 100;
		int numQueries = 50;
		int sampleSize = 2000;
		int split = Integer.parseInt(args[7]);
		boolean updateEveryHalf = Boolean.parseBoolean(args[8]);

		String updateMethod = args[4];
		//String updateMethod = "Window";
		//String updateMethod = "Inner";

		String sampling = args[5];
		//String sampling = "Explicit";
		//String sampling = "Query";

		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		//Directory directory = new RAMDirectory();
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationRealIndex"));
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationNewIndex"));
		Directory directory = new SimpleFSDirectory(new File(args[1]));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		//Set<String> collectionFixed = indexCollection(path,subPaths,conn);
		
		Directory directory_sent = new SimpleFSDirectory(new File(args[2]));
		IndexConnector conn_sent = new IndexConnector(analyzer, directory_sent, "");
		
		System.out.println("Num Sentences: " + conn_sent.getNumDocuments());
		
		for(String relationship : relationships){
			
			List<List<Tuple>> allTuples = new ArrayList<List<Tuple>>();
			
			String resultsPath = "results" + relationship;
			Set<String> knownDocuments = new HashSet<String>();
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

			System.out.println("Initiating IE programs");
			CompressedAdditiveFileSystemWrapping extractWrapper = new CompressedAdditiveFileSystemWrapping();
			for(String subPath : subPaths){
				if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationship + "-sentence.data");
				}else{
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + "-sentence.data");
				}
			}

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
			knownDocuments.addAll(sample);

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
			
			int sample_sentences = local_sents.size();
			
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
			OnlineRankingModel model = new OnlineRankingModel(coordinator, sents, relevantSents, engine, 10000);
			System.out.println(getTopKQueries(model,coordinator,10));
			Set<Pair<String,String>> sentQueries = new HashSet<Pair<String,String>>();

			System.out.println("Generating initial queries");
			Set<Pair<String,String>> currentQueries = getTopKQueries(model,coordinator,sentQueries,topQueriesInUpdate);
			System.out.println("Generating initial document collection");
			Set<String> knownSentencesToProcess = new HashSet<String>();
			for(Pair<String,String> pair : currentQueries){
				Query query;
				if(pair.first().equals("*")){
					query = qp.parse("+\"" + pair.second() + "\"");
				}else{	
					query = new TermQuery(new Term(pair.first(), pair.second()));
				}
				List<Integer> docsQuery;
				if(docsPerQuery!=-1){
					docsQuery = conn.search(query,docsPerQuery);
				}else{
					//More efficient method that does not sort the documents
					docsQuery = conn.searchWithoutOrder(query);
				}
				
				containQuery = new BooleanQuery();
				BooleanQuery.setMaxClauseCount(Math.max(1, docsQuery.size()));

				
				for(Integer docId : docsQuery){
					
					String docName = conn.getPath(docId);
					
					if(!knownDocuments.contains(docName)){
						
						containQuery.add( new TermQuery(new Term(DOCUMENT_FIELD, docName)), Occur.SHOULD);
					
						knownDocuments.add(docName);
						
					}
				}
				
				local_sents = conn_sent.search(containQuery);
				
				System.out.format("Docs: %d matched %d sentneces\n", docsQuery.size(),local_sents.size());
				
				for (Integer local_sent : local_sents) {
					
					String local_sent_str = conn_sent.getPath(local_sent);
					
					knownSentencesToProcess.add(local_sent_str);
					
				}

				
			}

			System.out.println("Transforming known documents to process into sentences");
			

			
			System.out.println("Performing initial ranking");
			//Map<String,Double> scoresCollection = model.getScores(collection);
			Map<Query,Double> queryScores = model.getQueryScores();
			Map<String,Float> scoresCollection = conn_sent.getScores(queryScores, new SimpleBooleanSimilarity(), knownSentencesToProcess, knownDocuments);
			List<String> initialRanking = sortCollection(scoresCollection);
			List<String> rankedCollection = new ArrayList<String>(initialRanking);

			System.out.println("Extracting information");
			Map<String,Integer> relevance = new HashMap<String, Integer>();
			List<String> adaptiveRanking = new ArrayList<String>();
			int collectionSize=rankedCollection.size();
			List<String> currentBatchSents = new ArrayList<String>();
			List<String> currentBatchRelSents = new ArrayList<String>();
			int numUpdates=0;

			System.out.println("Preparing update decider");
			UpdateDecision updateDecision = null;
			UpdatePrediction updatePrediction = null;
			if(updateMethod.equals("Window")){
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

			List<Integer> updates = new ArrayList<Integer>();
			int nextUpdate=collectionSize/2;
			for(int i=0; i<collectionSize; i++){
				String sent = rankedCollection.get(i);
				knownSentencesToProcess.remove(sent);
				adaptiveRanking.add(sent);
				currentBatchSents.add(sent);
				List<Tuple> tuples = extractWrapper.getTuplesDocument(sent);
				int num = tuples.size();
				if(num!=0){
					currentBatchRelSents.add(sent);
					relevance.put(sent, num);
					allTuples.add(tuples);
				}
				measurer.addCheckPoint();

				addTupleFeatures(qp, tuples);

				if((updateDecision != null && updateDecision.doUpdate(currentBatchSents, currentBatchRelSents)) 
				|| (updatePrediction != null && updatePrediction.predictUpdate(rankedCollection,i))
				|| (i==collectionSize-1)
				|| (updateEveryHalf && (i==nextUpdate && nextUpdate!=0))){
					updates.add(adaptiveRanking.size());
					submitTopTuples(conn_sent, coordinator, 100);
					System.out.println("\tUpdating ranking model " + (++numUpdates));
					model.updateModel(new HashSet<String>(currentBatchSents), new HashSet<String>(currentBatchRelSents), 1000);

					System.out.println("\tObtaining new Queries");
					Set<Pair<String,String>> newQueries = getTopKQueries(model,coordinator,sentQueries,topQueriesInUpdate);

					System.out.println("\tNumber of new queries: " + newQueries.size());

					System.out.println("\tSubmiting new Queries and obtaining new documents");
					int numNewDocs=0;
					for(Pair<String,String> pair : newQueries){
						Query query;
						if(pair.first().equals("*")){
							query = qp.parse("+\"" + pair.second() + "\"");
						}else{	
							query = new TermQuery(new Term(pair.first(), pair.second()));
						}
						List<Integer> docsQuery;
						if(docsPerQuery!=-1){
							docsQuery = conn.search(query,docsPerQuery);
						}else{
							//More efficient method that does not sort the documents
							docsQuery = conn.searchWithoutOrder(query);
						}
						
						containQuery = new BooleanQuery();
						BooleanQuery.setMaxClauseCount(Math.max(1, docsQuery.size()));
						
						for(Integer docId : docsQuery){
							
							String docName = conn.getPath(docId);
							
							if(!knownDocuments.contains(docName)){
								
								numNewDocs++;
								
								containQuery.add( new TermQuery(new Term(DOCUMENT_FIELD, docName)), Occur.SHOULD);
							
								knownDocuments.add(docName);
								
							}
						}
						
						local_sents = conn_sent.search(containQuery);
						
						System.out.format("Docs: %d matched %d sentneces\n", docsQuery.size(),local_sents.size());
						
						for (Integer local_sent : local_sents) {
							
							String local_sent_str = conn_sent.getPath(local_sent);
							
							knownSentencesToProcess.add(local_sent_str);
							
						}
					}
					nextUpdate=numNewDocs/2;

					System.out.println("Top features: ");
					System.out.println(getTopKQueries(model,coordinator,10));

					System.out.println("\tRe-ranking");
					//Map<String,Double> newScoresCollection = model.getScores(collection);
					Map<Query,Double> newQueryScores = model.getQueryScores();
					Map<String,Float> newScoresCollection = conn_sent.getScores(newQueryScores, new SimpleBooleanSimilarity(), knownSentencesToProcess, knownDocuments);
					rankedCollection = sortCollection(newScoresCollection);

					System.out.println("\tFinished Ranking - Back to extraction");
					System.out.println("\tWe have not processed " + rankedCollection.size() + " documents available.");

					i=-1;
					collectionSize=rankedCollection.size();
					
					
					if (updateDecision != null)
						updateDecision.reset();
					if (updatePrediction != null){
						updatePrediction.performUpdate(currentBatchSents, currentBatchRelSents);
					}
					
					currentBatchSents = new ArrayList<String>();
					currentBatchRelSents = new ArrayList<String>();
					
				}
			}

			Set<String> allDocs = conn.getAllFiles();
			
			allDocs.removeAll(knownDocuments);
			
			containQuery = new BooleanQuery();
			BooleanQuery.setMaxClauseCount(Math.max(1, allDocs.size()));
			
			for(String doc : allDocs){
				
				containQuery.add( new TermQuery(new Term(DOCUMENT_FIELD, doc)), Occur.SHOULD);
			
			}
			
			local_sents = conn_sent.search(containQuery);
			
			System.out.println("Last query matched: " + local_sents.size() + " sentences");
			System.out.println("Previously, there where: " + adaptiveRanking.size() + " processed sentences");
			System.out.println("Total from index: " + conn_sent.getNumDocuments());
			
			for (Integer local_sent : local_sents) {
				
				String local_sent_str = conn_sent.getPath(local_sent);
			
				int num = extractWrapper.getNumTuplesDocument(local_sent_str);
				if(num!=0){
					relevance.put(local_sent_str, num);
				}
				
			}
						
			System.out.println("Plotting results");
			int numDocs = (conn_sent.getNumDocuments()-sample_sentences);

			String docLimit = "";
			if(docsPerQuery!=-1){
				docLimit=""+docsPerQuery;
			}
			
			File folder = new File("resultsRank");
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/search-s" + docLimit);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/search-s" + docLimit + "/" + sampling);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/search-s" + docLimit + "/" + sampling + "/"+ updateMethod);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/search-s" + docLimit + "/" + sampling + "/"+ updateMethod + "/" + relationship);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/search-s" + docLimit + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split);
			if(!folder.exists()){
				folder.mkdir();
			}
			SerializationHelper.write("resultsRank/search-s" + docLimit + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/baseline.data", new BaselineCurve(numDocs));
			SerializationHelper.write("resultsRank/search-s" + docLimit + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/perfect.data", new PerfectCurve(numDocs, relevance));
			SerializationHelper.write("resultsRank/search-s" + docLimit + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/initialRankSVM.data", new SortedCurve(numDocs, initialRanking, relevance));
			SerializationHelper.write("resultsRank/search-s" + docLimit + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVM.data", new SortedCurve(numDocs, adaptiveRanking, relevance));
			SerializationHelper.write("resultsRank/search-s" + docLimit + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVMUpdates.updates", updates);
			SerializationHelper.write("resultsRank/search-s" + docLimit + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVMTimes.times", measurer.getCheckPoints());
			SerializationHelper.write("resultsRank/search-s" + docLimit + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVMtuples.tuples", allTuples);
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

	private static Set<Pair<String,String>> getTopKQueries(OnlineRankingModel model,
			FeaturesCoordinator coordinator, Set<Pair<String,String>> sentQueries, int k) {
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

		Set<Pair<String,String>> results = new HashSet<Pair<String,String>>();
		int queriesSize=queries.size();
		for(int i=0; i<Math.min(queriesSize, k); i++){
			Pair<String,String> query = queries.get(i);
			if(!sentQueries.contains(query)){
				results.add(query);
				sentQueries.add(query);
			}else{
				k++;
			}
		}

		return results;
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

	private static List<Query> loadQueries(QueryParser qp, String queryFile, int numQueries) throws ParseException, IOException {
		InitialWordLoader iwl = new FromWeightedFileInitialWordLoader(qp,queryFile);
		List<Query> words = iwl.getInitialQueries().subList(0, numQueries);
		return words;
	}

	private static List<Pair<String,String>> getTopKQueries(OnlineRankingModel model,
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

		List<Pair<String,String>> results = new ArrayList<Pair<String,String>>();
		int queriesSize=queries.size();
		for(int i=0; i<Math.min(queriesSize, k); i++){
			Pair<String,String> query = queries.get(i);
			results.add(query);
		}

		return results;
	}
}
