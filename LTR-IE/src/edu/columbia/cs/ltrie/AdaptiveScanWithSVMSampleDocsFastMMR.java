package edu.columbia.cs.ltrie;

import java.io.File;
import java.io.IOException;
import java.nio.channels.UnsupportedAddressTypeException;
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
import org.apache.lucene.search.similarities.DefaultSimilarity;
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
import weka.core.UnassignedClassException;

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
import edu.columbia.cs.ltrie.features.similarity.Similarity;
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

public class AdaptiveScanWithSVMSampleDocsFastMMR {
	
	private static final String DOCUMENT_FIELD = "ORIGINAL_DOCUMENT";
	
	
	public static void main(String[] args) throws Exception {

		System.out.println("Using fast version");
		
		float lambda = 0.3f;
		
		String task = ""; //For Validation
		
		if (args.length == 9){
			task = "_" + args[8];
		}
		
		String suffix = task + "-sentence";//"";
		
		TimeMeasurer measurer = new TimeMeasurer();
		//Declarations
		String path = args[0];
		//String path = "/home/goncalo/NYTValidationSplit/";
		
		String extractor = args[6];
		
		File pathF = new File(path);
		int numPaths= extractor.equals("OpenCalais") ? 100 : pathF.list().length;
		String[] subPaths = new String[numPaths];
		String folderDesign = "%0" + String.valueOf(numPaths).length() + "d";
		
		if (String.valueOf(numPaths).length() < 3){
			folderDesign = "%0" + 3 + "d";
		}
		
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format(folderDesign, i);
		}
		
		 //"Pablo-Dependency-Graph","Pablo-N-Grams","Pablo-Shortest-Path","Pablo-Sub-sequences"
		String[] relationships = new String[]{args[2]};
		//String[] relationships = new String[]{"Indictment-Arrest-Trial"};

		//String[] relationships = new String[]{"VotingResult"};
		int docsPerQuerySample = 10;
		int numQueries = 50;
		int sampleSize = 2000;
		int split = Integer.parseInt(args[5]);
		
		String updateMethod = args[3];
		//String updateMethod = "Window";
		//String updateMethod = "Inner";
		//String updateMethod = "FeatureRank";
		
		String sampling = args[4];
		//String sampling = "Explicit";
		//String sampling = "Query";

		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		
		Directory directory_docs = new SimpleFSDirectory(new File(args[7]));
		IndexConnector conn_docs = new IndexConnector(analyzer, directory_docs, "");

		
		//analyzer = new ShingleAnalyzerWrapper(analyzer, 2, 2);
		//Directory directory = new RAMDirectory();
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationRealIndex"));
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationNewIndex"));
		Directory directory = new SimpleFSDirectory(new File(args[1]));
		//Directory directory = new MMapDirectory(new File(args[1]));
		//Directory directory = new SimpleFSDirectory(new File(args[1]), NoLockFactory.getNoLockFactory());
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		//Set<String> collectionFixed = indexCollection(path,subPaths,conn);
		
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
				
				if (extractor.equals("OpenCalais")){
					extr = extractor + "-relationWords_Ranker_";
					initialQueriesPath = "QUERIES-OC/TREC/" + relationship + "/" + false + "/SelectedAttributes/" + extr + featSel + "_"+split+"_5000.words";
				} else{
					extr = getShortExtractor(extractor) + "-"+relationship+"-SF-"+(relationship.equals("ManMadeDisaster")? "HMM":"CRF")+"-relationWords_Ranker_";
					initialQueriesPath = "QUERIES/" + relationship + "/" + true + "/SelectedAttributes/" + extr + featSel + "_"+split+"_5000.words";
				}
				
			}
			List<Query> initialQueries = loadQueries(qp, initialQueriesPath,numQueries);

			System.out.println(initialQueries.size());
			
			//Set<String> collection=new HashSet<String>(collectionFixed);
			String resultsPath = "results" + relationship;
			System.out.println("Initiating IE programs");
			CompressedAdditiveFileSystemWrapping extractWrapper = new CompressedAdditiveFileSystemWrapping();
			for(String subPath : subPaths){
				if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationship + suffix + ".data");
				}else{
					
//					System.out.println("REMOVE THIS!");
//					if (subPath.equals("016") || subPath.equals("067"))
//						continue;
					
					if (extractor.equals("OpenCalais")){
						//I only stored the non-empty
						if (new File(resultsPath + "/" + subPath + task + "_" + extractor + "_" + relationship + suffix.replace(task, "") + ".data").exists()) 
							extractWrapper.addFiles(resultsPath + "/" + subPath + task + "_" + extractor + "_" + relationship + suffix.replace(task, "") + ".data");
						
					}else{
						extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + suffix + ".data");
					}
					
				}
			}
			
			List<List<Tuple>> allTuples = new ArrayList<List<Tuple>>();
			
			System.out.println("Preparing feature extractors");
			FeaturesCoordinator coordinator = new FeaturesCoordinator();
			FeaturesCoordinator mmrcoordinator = new FeaturesCoordinator();
			Set<String> fields = new HashSet<String>();
			for(String field : fieldsVector){
				fields.add(field);
			}
			//coordinator.addFeatureExtractor(new AllFieldsTermFrequencyFeatureExtractor(conn, fields, false,false,true));
			coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.ALL_FIELD,false,false,true));
			mmrcoordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.ALL_FIELD,false,false,true));
			//coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.LEAD_FIELD,false,false,true));
			//coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.BODY_FIELD,false,false,true));
			for(Query q : initialQueries){
				Set<Term> terms = new HashSet<Term>();
				q.extractTerms(terms);
				if(terms.size()>1){
					coordinator.addFeatureExtractor(new MatchesQueryFeatureExtractor(conn, q));
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
				sampler = new InitialQuerySamplingTechnique(conn_docs, qp, initialQueriesPath, docsPerQuerySample,sampleSize);
			}else if (sampling.equals("Smart")){
				sampler = new CyclicInitialQuerySamplingTechnique(conn_docs, qp, initialQueriesPath, docsPerQuerySample,numQueries,sampleSize);
			}else{
				throw new UnsupportedOperationException("No sampling parameter: '" + sampling + "'");
			}
			List<String> sample_docs = sampler.getSample();
			
			conn_docs.closeReader();
			
			BooleanQuery containQuery = new BooleanQuery();
			BooleanQuery.setMaxClauseCount(Math.max(1, sample_docs.size()));
			
			for(String doc : sample_docs){
				
				if (extractor.equals("OpenCalais"))
					doc += ".rdf";
				
				containQuery.add( new TermQuery(new Term(DOCUMENT_FIELD, doc)), Occur.SHOULD);
			}
			
			List<Integer> local_sents = conn.search(containQuery);
			
			System.out.format("Docs: %d matched %d sentneces\n", sample_docs.size(),local_sents.size());
			
			System.out.println("Extracting information from the sample");
			List<String> relevantDocs = new ArrayList<String>();
			List<String> docs = new ArrayList<String>();
			
			
			for (Integer local_sent : local_sents) {
				
				String local_sent_str = conn.getPath(local_sent);
				
				List<Tuple> tuples = extractWrapper.getTuplesDocument(local_sent_str);
				if(tuples.size()!=0){
					relevantDocs.add(local_sent_str);
					allTuples.add(tuples);
				}
				docs.add(local_sent_str);
				addTupleFeatures(qp, tuples);
				
			}
			
			collection.removeAll(docs);
			System.out.println("\tThe collection without the sample contains " + collection.size() + " documents.");
			
			System.out.println("\tThere are " +relevantDocs.size() +" relevant documents in the sample.");
			System.out.println("\tThere are " +(docs.size()-relevantDocs.size()) +" non relevant documents in the sample.");

			System.out.println("Initial training of the ranking model");
			LinearOnlineEngine<Long> engine = new ElasticNetLinearPegasosEngine<Long>(0.1,0.99, 1, false);
			//LinearOnlineEngine<Long> engine = new LinearPegasosEngine<Long>(0.1, 1.0,false);
			OnlineRankingModel model = new OnlineRankingModel(coordinator, docs, relevantDocs, engine, 10000);

			System.out.println(getTopKQueries(model,coordinator,10));
			
			
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

			System.out.println("Preparing update decider");
			UpdateDecision updateDecision = null;
			UpdatePrediction updatePrediction = null;
			if(updateMethod.equals("Window")){
				updateDecision = new ExactWindowUpdateDecision(collectionSize/50);
			}else if(updateMethod.equals("Shifting")){
				updatePrediction = new FeatureShiftOnline(docs, relevantDocs, conn, coordinator, 1000/*instances to look*/);
			}else if(updateMethod.equals("FeatureRank")){
				updateDecision = new FeatureRankOnline(new ArrayList<String>(docs), new ArrayList<String>(relevantDocs), coordinator, 0.05, 200);
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
			int updateevery = 25;
			int mmrnRel = 0;
			boolean canupdate=false;
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
					rel++;
					mmrnRel++;
					canupdate=true;
					allTuples.add(tuples);
				}else{
					nRel++;
				}
				measurer.addCheckPoint();

				addTupleFeatures(qp, tuples);

				if((updateDecision != null && updateDecision.doUpdate(currentBatchDocs, currentBatchRelDocs)) || (updatePrediction != null && updatePrediction.predictUpdate(rankedCollection,i))){
					updates.add(adaptiveRanking.size());
					submitTopTuples(conn, coordinator, 100);
					System.out.println("\tUpdating ranking model " + (++numUpdates));
					Map<Long, Double> oldVector = new HashMap<Long, Double>();
					for (Entry<Long, Double> entry : model.getWeightVector().entrySet()) {
						oldVector.put(entry.getKey(), entry.getValue());
					}
					model.updateModel(new HashSet<String>(currentBatchDocs), new HashSet<String>(currentBatchRelDocs), currentBatchDocs.size());
					updateStatistics.add(new UpdateStatistics(oldVector, model.getWeightVector()));
					System.out.println("New model uses " + model.getWeightVector().size() + " features.");
					System.out.println("Top features: ");
					System.out.println(getTopKQueries(model,coordinator,10));
					System.out.println("\tRe-ranking");
					//Map<String,Double> newScoresCollection = model.getScores(collection);
					Map<Query,Double> newQueryScores = model.getQueryScores();
					Map<String,Float> newScoresCollection = conn.getScores(newQueryScores, new SimpleBooleanSimilarity(), collection);
//					Map<String,Float> simScores = getMaxSimilarityScores(conn,mmrcoordinator,newScoresCollection.keySet(),adaptiveRanking);
					
//					rankedCollection = sortCollectionMMR(newScoresCollection,simScores,lambda);
	
					rankedCollection = sortCollection(newScoresCollection);
					
					
					System.out.println("\tFinished Ranking - Back to extraction");
					System.out.println("\t" + rankedCollection.size() + " documents to go.");
					
					

					i=-1;
					collectionSize=rankedCollection.size();
					

					if (updateDecision != null)
						updateDecision.reset();
					if (updatePrediction != null){
						updatePrediction.performUpdate(currentBatchDocs, currentBatchRelDocs);
					}
					
					currentBatchDocs = new ArrayList<String>();
					currentBatchRelDocs = new ArrayList<String>();
				} else if (canupdate && mmrnRel % updateevery == 0){
					canupdate=false;
					System.out.println("Updating");
					Map<Query,Double> newQueryScores = model.getQueryScores();
					Map<String,Float> newScoresCollection = conn.getScores(newQueryScores, new SimpleBooleanSimilarity(), collection);
					Map<String,Float> simScores = getMaxSimilarityScores(conn,mmrcoordinator,newScoresCollection.keySet(),relevance.keySet());
					rankedCollection = sortCollectionMMR(newScoresCollection,simScores,lambda);
					i=-1;
					collectionSize=rankedCollection.size();
				}
				
				int currentNumberOfProcessedDocs = adaptiveRanking.size();
				if(currentNumberOfProcessedDocs%1000==0){
					System.out.println("Processed " + currentNumberOfProcessedDocs + " documents (" + (rel*100.0/(rel+nRel))+ "% precision)");
				}
			}
			
			System.out.println("Final feature space size: " + coordinator.getCurrentNumberFeatures());

			System.out.println("Plotting results");
			//ExcelGenerator gen = new ExcelGenerator();
			int numDocs = adaptiveRanking.size();
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
			folder = new File("resultsRank/full" + suffix);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/full" + suffix + "/" + sampling);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/full" + suffix + "/" + sampling + "/"+ updateMethod);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/full" + suffix + "/" + sampling + "/"+ updateMethod + "/" + relationship);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/full" + suffix + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor);
			if(!folder.exists()){
				folder.mkdir();
			}
			
			folder = new File("resultsRank/full" + suffix + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split);
			if(!folder.exists()){
				folder.mkdir();
			}
						
			SerializationHelper.write("resultsRank/full" + suffix + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/initialRankSVM_SamD-MMR.data", new SortedCurve(numDocs, initialRanking, relevance));
			SerializationHelper.write("resultsRank/full" + suffix + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split+ "/adaptiveRankSVM_SamD-MMR.data", new SortedCurve(numDocs, adaptiveRanking, relevance));
			SerializationHelper.write("resultsRank/full" + suffix + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVMUpdates_SamD-MMR.updates", updates);
			SerializationHelper.write("resultsRank/full" + suffix + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVMUpdates.updates_SamD-MMR.statistics", updateStatistics);
			SerializationHelper.write("resultsRank/full" + suffix + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVMTimes_SamD-MMR.times", measurer.getCheckPoints());
			SerializationHelper.write("resultsRank/full" + suffix + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVMtuples_SamD-MMR.tuples", allTuples);
			if(updateDecision!=null){
				System.out.println(updateDecision.report());
			}
			if(updatePrediction!=null){
				System.out.println(updatePrediction.report());
			}
		}
	}

	private static Map<String, Float> getMaxSimilarityScores(
			IndexConnector conn, FeaturesCoordinator mmrcoordinator, Set<String> docsToSelect,
			Set<String> setDocs) throws IOException, ParseException {
		
		Map<String,Float> ret = new HashMap<String, Float>();
		
		org.apache.lucene.search.similarities.Similarity s = new DefaultSimilarity();
		
		HashSet<String> noDocs = new HashSet<String>(0);
		
		List<String> docs = new ArrayList<String>(setDocs);
		Collections.shuffle(docs);
		
		setDocs = new HashSet<String>(docs.subList(0, Math.min(75, docs.size())));
		
		int don = 1;
		
		for (String doc : setDocs) {

			System.out.println("Doc: " + don++);
			
			Map<Long, Double> feats = mmrcoordinator.getFeatures(doc);
			
			Map<Query,Double>weights = new HashMap<Query, Double>(feats.size());
			
			for (Entry<Long,Double> feat : feats.entrySet()) {
			
				weights.put(mmrcoordinator.getQueries(feat.getKey()),feat.getValue());
				
			}
			
			List<edu.columbia.cs.utils.Pair<String, Float>> res = conn.getScoresOrdered(weights, s, docsToSelect,noDocs);
			
			for (edu.columbia.cs.utils.Pair<String, Float> pair : res) {
				
				Float v = ret.get(pair.first());
				
				if (v == null || pair.second() > v){
					ret.put(pair.first(), pair.second());
				}
				
			}
			
			
		}
		
		for (String string : docsToSelect) {
			
			if (!ret.containsKey(string)){
				ret.put(string, 0.0f);
			}
			
		}
		
		return ret;
		
	}

	public static String getShortExtractor(String extractor) {
		
		if (extractor.equals("Pablo-N-Grams")){
			return "BONG";
		}else if (extractor.equals("Pablo-Sub-sequences")){
			return "SSK";
		}
		
		throw new UnassignedClassException("Not quite ready extractor");
		
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

	private static List<String> sortCollectionMMR(Map<String,Float> scoresCollection, Map<String, Float> simScores, final float lambda){
		final Map<String,Float> scores = scoresCollection;
		final Map<String,Float> scores2 = simScores;
		List<String> collection = new ArrayList<String>(scoresCollection.keySet());
		Collections.sort(collection, new Comparator<String>() {
			@Override
			public int compare(String doc1, String doc2) {
				Float score2 = scores.get(doc2); 
				if(score2==null){
					score2=0.0f;
				}else{
					
					score2=lambda * scores.get(doc2) - (1.0f - lambda)*scores2.get(doc2);
				}
				Float score1 = scores.get(doc1);
				if(score1==null){
					score1=0.0f;
				}else{
					score1=lambda * scores.get(doc1) - (1.0f - lambda)*scores2.get(doc1);
				}
				return (int) Math.signum(score2-score1);
			}
		});
		return collection;
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
