package edu.columbia.cs.ltrie.samples;

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

public class SampleDocLevel {

	private static final String DOCUMENT_FIELD = "ORIGINAL_DOCUMENT";

	public static void main(String[] args) throws Exception {

		String path = args[0];
		Directory directory = new SimpleFSDirectory(new File(args[1]));
		Directory directory_sent = new SimpleFSDirectory(new File(args[2]));
		String[] relationships = new String[]{args[3]};
		String sampling = args[5];
		int split = Integer.parseInt(args[6]);

		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		//Directory directory = new RAMDirectory();
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationRealIndex"));
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationNewIndex"));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		//Set<String> collectionFixed = indexCollection(path,subPaths,conn);

		IndexConnector conn_sent = new IndexConnector(analyzer, directory_sent, "");

		File pathF = new File(path);
		int numPaths=pathF.list().length;

		String[] subPaths = new String[numPaths];
		String folderDesign = "%0" + String.valueOf(numPaths).length() + "d";
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format(folderDesign, i);
		}
		String extractor = "Pablo-Sub-sequences";

		int docsPerQuerySample = 10;
		int numQueries = 50;
		int sampleSize = 2000;

		for(String relationship : relationships){

			String resultsPath = "results" + relationship;

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

			System.out.println("Initiating IE programs");
			CompressedAdditiveFileSystemWrapping extractWrapper = new CompressedAdditiveFileSystemWrapping();
			for(String subPath : subPaths){
				if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationship + "-sentence.data");
				}else{
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + "-sentence.data");
				}
			}


			System.out.println("Obtaining initial sample (to use Pablo's sampling techniques)");
			SamplingTechnique sampler;

			if (sampling.equals("Smart")){
				sampler = new CyclicInitialQuerySamplingTechnique(conn, qp, initialQueriesPath, docsPerQuerySample,numQueries,sampleSize);
			}else{
				throw new UnsupportedOperationException("No sampling parameter: '" + sampling + "'");
			}
			List<String> sample = sampler.getSample();
			System.out.println("\tThe sample contains " + sample.size() + " documents.");

			System.out.println("Extracting information from the sample");
			List<String> relevantSents = new ArrayList<String>();
			List<String> sents = new ArrayList<String>();

			BooleanQuery containQuery = new BooleanQuery();
			BooleanQuery.setMaxClauseCount(Math.max(1, sample.size()));

			for(String doc : sample){

				System.out.println(doc);
				
				containQuery.add( new TermQuery(new Term(DOCUMENT_FIELD, doc)), Occur.SHOULD);
			}

			List<Integer> local_sents = conn_sent.search(containQuery);

			System.out.format("Docs: %d matched %d sentneces\n", sample.size(),local_sents.size());

			Set<String> fields = new HashSet<String>();
			
			fields.add(NYTDocumentWithFields.ALL_FIELD);
			
			for (Integer local_sent : local_sents) {

				String local_sent_str = conn_sent.getPath(local_sent);

				List<Tuple> tuples = extractWrapper.getTuplesDocument(local_sent_str);
				if(tuples.size()!=0){
					relevantSents.add(local_sent_str);
					System.out.println(local_sent_str);
					System.out.println(tuples.toString());
					System.out.println(conn_sent.getTermFrequencies( local_sent_str, NYTDocumentWithFields.ALL_FIELD));
					
				}
				sents.add(local_sent_str);

				
				
			}

			System.out.println("\tThere are " +relevantSents.size() +" relevant sentences in the sample.");
			System.out.println("\tThere are " +(sents.size()-relevantSents.size()) +" non relevant sentences in the sample.");

		}

	}
	
}
