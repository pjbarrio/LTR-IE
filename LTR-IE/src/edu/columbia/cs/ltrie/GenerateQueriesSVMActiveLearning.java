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

import edu.columbia.cs.ltrie.active.learning.ActiveLearningOnlineModel;
import edu.columbia.cs.ltrie.active.learning.classifier.util.Combiner;
import edu.columbia.cs.ltrie.active.learning.classifier.util.impl.MaxCombiner;
import edu.columbia.cs.ltrie.active.learning.classifier.util.impl.SumCombiner;
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

public class GenerateQueriesSVMActiveLearning {
	public static void main(String[] args) throws Exception {
		TimeMeasurer measurer = new TimeMeasurer();
		if (args.length != 11){
			System.out.println("[Window] [Explicit|Query] [Sum|Max] Independent[true/false] split[1..5] relationship epochs[5] [L1|L2] lambda[0.5|0.1] [New|Real] docsPerQuery[-1|10]");
		}
		
		String updateMethod = args[2];
		String sampling = args[3]; //Smart
		String comb = args[4];
		boolean independent = Boolean.valueOf(args[5]); //submit each classifier independently
		int numQueries = 50;
		int split = Integer.valueOf(args[6]);
		String[] relationships = {args[7]};//{/*"ManMadeDisaster"/*"Indictment-Arrest-Trial"*/"OrgAff"};
		int numEpochs = Integer.valueOf(args[8]);
		String reg = args[9];
		Double lambda = Double.valueOf(args[10]);
		String ind = args[11];
		//If this value is -1 we do not care about the docs per query... There are scenarios in which
		//this cannot happen
		int docsPerQuery = Integer.valueOf(args[12]);
		boolean updateEveryHalf = false;
		int docsPerQuerySample = 10;
		int topQueriesInUpdate = 100;
		int sampleSize = 2000;
		
		
		String path = args[0];
		File pathF = new File(path);
		int numPaths=pathF.list().length;
		String[] subPaths = new String[numPaths];
		String folderDesign = "%0" + String.valueOf(numPaths).length() + "d";
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format(folderDesign, i);
		}
		String extractor = "Pablo-Sub-sequences";
		//String[] relationships = new String[]{"Indictment-Arrest-Trial"};
		
		
		

		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		//Directory directory = new RAMDirectory();
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationRealIndex"));
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationNewIndex"));
		Directory directory = new SimpleFSDirectory(new File(args[1]));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		//Set<String> collectionFixed = indexCollection(path,subPaths,conn);
		Set<String> collectionFixed = conn.getAllFiles();

		for(String relationship : relationships){
			String resultsPath = "results" + relationship;
			Set<String> collection=new HashSet<String>(collectionFixed);
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
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationship + ".data");
				}else{
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + ".data");
				}
			}
			
			System.out.println("Preparing feature extractors");
			FeaturesCoordinator coordinator = new FeaturesCoordinator();
			Set<String> fields = new HashSet<String>();
			for(String field : fieldsVector){
				fields.add(field);
			}
			//coordinator.addFeatureExtractor(new AllFieldsTermFrequencyFeatureExtractor(conn, fields, false,false,true));
			coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.ALL_FIELD,false,false,true));
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
				sampler = new InitialQuerySamplingTechnique(conn, qp, initialQueriesPath, docsPerQuerySample,sampleSize);
			}else if (sampling.equals("Smart")){
				sampler = new CyclicInitialQuerySamplingTechnique(conn, qp, initialQueriesPath, docsPerQuerySample,numQueries,sampleSize);
			}else{
				throw new UnsupportedOperationException("No sampling parameter: '" + sampling + "'");
			}
			
			List<String> sample = sampler.getSample();
			System.out.println("\tThe sample contains " + sample.size() + " documents.");
			knownDocuments.addAll(sample);
			System.out.println("\tThe collection without the sample contains " + (collection.size()-sample.size()) + " documents.");
			
			System.out.println("Extracting information from the sample");
			List<String> relevantDocs = new ArrayList<String>();
			List<String> docs = new ArrayList<String>();
			for(String doc : sample){
				List<Tuple> tuples = extractWrapper.getTuplesDocument(doc);
				if(tuples.size()!=0){
					relevantDocs.add(doc);
				}
				docs.add(doc);
				addTupleFeatures(qp, tuples);

			}
			System.out.println("\tThere are " +relevantDocs.size() +" relevant documents in the sample.");
			System.out.println("\tThere are " +(docs.size()-relevantDocs.size()) +" non relevant documents in the sample.");

			
			System.out.println("Initial training of the ranking model");
			
			Combiner combi =  comb.equals("Max")? new MaxCombiner() : new SumCombiner();
		    
		    ActiveLearningOnlineModel model = new ActiveLearningOnlineModel(docs,relevantDocs,coordinator,numEpochs,combi,reg,lambda,true,true);
			
			Set<Pair<String,String>> sentQueries = new HashSet<Pair<String,String>>();

			System.out.println("Generating initial queries");
			Set<Pair<String,String>> currentQueries = getTopKQueries(model,coordinator,sentQueries,topQueriesInUpdate);
			System.out.println("Generating initial document collection");
			Set<String> knownDocumentsToProcess = new HashSet<String>();
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
				for(Integer docId : docsQuery){
					String docName = conn.getPath(docId);
					if(!knownDocuments.contains(docName)){
						knownDocumentsToProcess.add(docName);
						knownDocuments.add(docName);
					}
				}
			}

			System.out.println("Performing initial ranking");
			//Map<String,Double> scoresCollection = model.getScores(collection);
			Map<String,Double> scoresCollection = model.getScores(knownDocumentsToProcess,conn,independent);
			
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
				updateDecision = new ModelSimilarityUpdateDecision(model, 30, 0.1);
			}else{
				throw new UnsupportedOperationException("No update parameter: '" + sampling + "'");
			}

			List<Integer> updates = new ArrayList<Integer>();
			int nextUpdate=collectionSize/2;
			for(int i=0; i<collectionSize; i++){
				String doc = rankedCollection.get(i);
				knownDocumentsToProcess.remove(doc);
				adaptiveRanking.add(doc);
				currentBatchDocs.add(doc);
				List<Tuple> tuples = extractWrapper.getTuplesDocument(doc);
				int num = tuples.size();
				if(num!=0){
					currentBatchRelDocs.add(doc);
					relevance.put(doc, num);
				}
				measurer.getCheckPoints();

				addTupleFeatures(qp, tuples);
				
				if((updateDecision != null && updateDecision.doUpdate(currentBatchDocs, currentBatchRelDocs)) 
						|| (updatePrediction != null && updatePrediction.predictUpdate(rankedCollection,i))
						|| (i==collectionSize-1)
						|| (updateEveryHalf && (i==nextUpdate && nextUpdate!=0))){
					if(i==collectionSize-1){
						System.out.println("THIS WAS THE LAST DOC!!!!");
					}
					updates.add(adaptiveRanking.size());
					submitTopTuples(conn, coordinator, 100);
					System.out.println("\tUpdating ranking model " + (++numUpdates));
					model.updateModel(currentBatchDocs, currentBatchRelDocs, 1000);

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
						for(Integer docId : docsQuery){
							String docName = conn.getPath(docId);
							if(!knownDocuments.contains(docName)){
								knownDocumentsToProcess.add(docName);
								knownDocuments.add(docName);
								numNewDocs++;
							}
						}
					}
					nextUpdate=numNewDocs/2;
					
					System.out.println("Top features: ");
					System.out.println(getTopKQueries(model,coordinator,10));

					System.out.println("\tRe-ranking");
					//Map<String,Double> newScoresCollection = model.getScores(collection);
					Map<String,Double> newScoresCollection = model.getScores(knownDocumentsToProcess,conn,independent);
					rankedCollection = sortCollection(newScoresCollection);

					System.out.println("\tFinished Ranking - Back to extraction");
					System.out.println("\tWe have not processed " + rankedCollection.size() + " documents available.");

					i=-1;
					collectionSize=rankedCollection.size();
					
					
					if (updateDecision != null)
						updateDecision.reset();
					if (updatePrediction != null){
						updatePrediction.performUpdate(currentBatchDocs, currentBatchRelDocs);
					}
					
					currentBatchDocs = new ArrayList<String>();
					currentBatchRelDocs = new ArrayList<String>();
				}
			}

			for(String doc : collection){
				if(!knownDocuments.contains(doc)){
					int num = extractWrapper.getNumTuplesDocument(doc);
					if(num!=0){
						relevance.put(doc, num);
					}
				}
			}
			
			System.out.println("Plotting results");
			int numDocs = (collection.size()-sample.size());
			
			String docLimit = "";
			if(docsPerQuery!=-1){
				docLimit=""+docsPerQuery;
			}

			File folder = new File("resultsRank");
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/search" + docLimit);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/search" + docLimit + "/" + sampling);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/search" + docLimit + "/" + sampling + "/"+ updateMethod);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/search" + docLimit + "/" + sampling + "/"+ updateMethod + "/" + relationship);
			if(!folder.exists()){
				folder.mkdir();
			}
			folder = new File("resultsRank/search" + docLimit + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split);
			if(!folder.exists()){
				folder.mkdir();
			}
			SerializationHelper.write("resultsRank/search" + docLimit + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/initialActiveLearning.data", new SortedCurve(numDocs, initialRanking, relevance));
			SerializationHelper.write("resultsRank/search" + docLimit + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveActiveLearning.data", new SortedCurve(numDocs, adaptiveRanking, relevance));
			SerializationHelper.write("resultsRank/search" + docLimit + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveActiveLearningUpdates.updates", updates);
			SerializationHelper.write("resultsRank/search" + docLimit + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveActiveLearningTimes.times", measurer.getCheckPoints());
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

	private static Set<Pair<String,String>> getTopKQueries(ActiveLearningOnlineModel model,
			FeaturesCoordinator coordinator, Set<Pair<String,String>> sentQueries, int k) {
		
		Map<Pair<String,String>, Double> termWeights = model.getTermWeights();

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

	private static List<String> sortCollection(Map<String,Double> scoresCollection){
		final Map<String,Double> scores = scoresCollection;
		List<String> collection = new ArrayList<String>(scoresCollection.keySet());
		Collections.sort(collection, new Comparator<String>() {
			@Override
			public int compare(String doc1, String doc2) {
				Double score2 = scores.get(doc2);
				if(score2==null){
					score2=0.0d;
				}
				Double score1 = scores.get(doc1);
				if(score1==null){
					score1=0.0d;
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
	
	private static List<Pair<String,String>> getTopKQueries(ActiveLearningOnlineModel model,
			FeaturesCoordinator coordinator, int k) {
		
		Map<Pair<String,String>, Double> termWeights = model.getTermWeights();
		
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
