package edu.columbia.cs.ltrie.samples;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
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

public class GenerateSplitsAsSamples {
	
	private static final String DOCUMENT_FIELD = "ORIGINAL_DOCUMENT";
	
	
	public static void main(String[] args) throws Exception {
		
		// /proj/db-files2/NoBackup/pjbarrio/Dataset/NYTTrain/ NaturalDisaster Pablo-Dependency-Graph /proj/db-files2/NoBackup/pjbarrio/Dataset/indexes/NYTTrain100Index Train
// /proj/db-files2/NoBackup/pjbarrio/Dataset/NYTTrain/ PersonParty OpenCalais /proj/db-files2/NoBackup/pjbarrio/Dataset/indexes/NYTTrain100Index Train
		
		String task = "_" + args[4];
		
		//Declarations
		String path = args[0];
		//String path = "/home/goncalo/NYTValidationSplit/";
		
		String extractor = args[2];
		
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
		String[] relationships = new String[]{args[1]};
		//String[] relationships = new String[]{"Indictment-Arrest-Trial"};


		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		
		
		for(String relationship : relationships){
			
			//Set<String> collection=new HashSet<String>(collectionFixed);
			String resultsPath = "results" + relationship;
			System.out.println("Initiating IE programs");
			CompressedAdditiveFileSystemWrapping extractWrapper = new CompressedAdditiveFileSystemWrapping();
			for(String subPath : subPaths){
				if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationship + ".data");
				}else{
					
					if (extractor.equals("OpenCalais")){
						if (new File(resultsPath + "/" + subPath + task + "_" + extractor + "_" + relationship + "-sentence.data").exists())
							extractWrapper.addFiles(resultsPath + "/" + subPath + task + "_" + extractor + "_" + relationship + "-sentence.data");
						
					}else{
						extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + task + ".data");
					}
					
				}
			}
			
			//generate
			
			Directory directory_docs = new SimpleFSDirectory(new File(args[3]));
			IndexConnector conn_docs = new IndexConnector(analyzer, directory_docs, "");

			Set<String> collection = conn_docs.getAllFiles();
//			Set<String> collectionFixed = conn.getAllFiles();

			
			Set<String> relevantDocs = (Set<String>)extractWrapper.getAllDocs();
			
			if (extractor.equals("OpenCalais")){
				
				Set<String> relc = new HashSet<String>();
				
				for (String string : relevantDocs) {
					
					relc.add(string.substring(0, string.indexOf(".rdf")));
					
				}
				
				relevantDocs = relc;
				
			}
			
			System.out.println(collection.size());
			
			collection.removeAll(relevantDocs); 
			
			System.out.println(relevantDocs.size() + " - " + collection.size());
			
			for (int spl = 1; spl <= 5; spl++) {
				
				List<String> sample = new ArrayList<String>(relevantDocs);
				
				double size = relevantDocs.size();
				
				List<String> allrest = new ArrayList<String>(collection);
				
				Collections.shuffle(allrest,new Random(spl));
				
				for (double i = 90.0; i >= 10.0; i-=10.0) {
										
					int ulesscount = (int)((100.0 - i)*size/i);
					
					int needs = ulesscount - (sample.size() - (int)size);
					
					while (needs-->0){
						sample.add(allrest.remove(0));
						
					}
					
					System.out.println("\tThe sample contains " + sample.size() + " documents for " + i);
					
					SerializationHelper.write(getSplitName(extractor,relationship,spl,i), sample);
					
					
				}
				
			}
						
		}
	}


	private static String getSplitName(String extractor,
			String relationship, int spl, double perc) {
		
		return "splits/" + extractor + "." + relationship + "." + perc + "." + spl + ".ser";
		
	}


	
}
