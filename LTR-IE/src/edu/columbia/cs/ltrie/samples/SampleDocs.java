package edu.columbia.cs.ltrie.samples;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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

import cc.mallet.extract.Extractor;

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

public class SampleDocs {

	public final static String[] reelrelations = {"NaturalDisaster","VotingResult","ManMadeDisaster","PersonCareer","Indictment-Arrest-Trial"};
	
	public static String[] relations = {"PersonParty","CompanyLocation","FamilyRelation","PersonAttributes","Extinction",
		"PoliticalRelationship","EnvironmentalIssue","PersonTravel",
		"PersonCareer","ProductRecall","CompanyLaborIssues","VotingResult","CompanyLegalIssues",
		"PersonLocation","IPO","CompanyMeeting","NaturalDisaster","CandidatePosition","Quotation","CompanyAffiliates",
		"DiplomaticRelations","ContactDetails","AnalystRecommendation","Buybacks","PatentFiling","CompanyInvestment",
		"CompanyLayoffs","Conviction","Indictment","EmploymentChange","ConferenceCall","Bankruptcy","StockSplit",
		"Dividend","CompanyCompetitor","CompanyEmployeesNumber","Trial","CompanyNameChange","DelayedFiling",
		"PoliticalEndorsement","CreditRating","BusinessRelation","BonusSharesIssuance","Acquisition",
		"CompanyForceMajeure","CompanyProduct","PersonCommunication","ArmedAttack","CompanyUsingProduct",
		"IndicesChanges","CompanyEarningsAnnouncement","MusicAlbumRelease","CompanyTechnology",
		"CompanyExpansion","CompanyFounded","AnalystEarningsEstimate","PersonEducation","PatentIssuance",
		"JointVenture","Arrest","MovieRelease","PersonEmailAddress","FDAPhase","SecondaryIssuance","GenericRelations",
		"CompanyRestatement","EquityFinancing","ManMadeDisaster","ArmsPurchaseSale","MilitaryAction","ProductIssues",
		"Alliance","DebtFinancing",
		"CompanyTicker","CompanyReorganization","CompanyAccountingChange","Merger",
		"EmploymentRelation","ProductRelease","CompanyListingChange","PersonRelation","CompanyEarningsGuidance",
		"PollsResult","CompanyCustomer"};
	
	public static void main(String[] args) throws Exception {

		String folderDesign = "%0" + 3 + "d";
		
		String parentPath = "/proj/db-files2/NoBackup/pjbarrio/Dataset/NYTTrain/";
		
		String dir_name = args[0];
		
		Directory directory = new SimpleFSDirectory(new File(dir_name));
		String[] relationships = new String[]{args[1]};
		String extractor = args[2]; //e.g., SSK, BONG, OpenCalais
//		if (extractor.equals("OpenCalais")){
//			relationships = relations;
//		} else {
//			relationships = reelrelations;
//		}
		String sampling = args[3];
		int split = Integer.parseInt(args[4]);
		int docsPerQuerySample = Integer.valueOf(args[5]);//10;
		int numQueries = Integer.valueOf(args[6]);//50;
		int sampleSize = Integer.valueOf(args[7]);//2000;
		
		
		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		//Directory directory = new RAMDirectory();
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationRealIndex"));
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationNewIndex"));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		//Set<String> collectionFixed = indexCollection(path,subPaths,conn);

		

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
					extr = extractor + "-"+relationship+"-SF-"+(relationship.equals("ManMadeDisaster")? "HMM":"CRF")+"-relationWords_Ranker_";
					initialQueriesPath = "QUERIES/" + relationship + "/" + true + "/SelectedAttributes/" + extr + featSel + "_"+split+"_5000.words";
				}
				
			}

			System.out.println("Obtaining initial sample (to use Pablo's sampling techniques)");
			SamplingTechnique sampler;

			String sampleFile = null;
			
			if (sampling.equals("Smart")){
				sampler = new CyclicInitialQuerySamplingTechnique(conn, qp, initialQueriesPath, docsPerQuerySample,numQueries,sampleSize);
				sampleFile = getSampleFile(dir_name,"DocumentSampler",relationship, extractor, sampleSize, sampling, split, docsPerQuerySample, numQueries);
			} else if(sampling.equals("Explicit")){
				String[] documents = new String[2];
				documents[0] = String.format(folderDesign, split*2-1);
				documents[1] = String.format(folderDesign, split*2);

				sampler = new ExplicitSamplingTechnique(parentPath, documents);
				sampleFile = getSampleFile(dir_name,"DocumentSampler", sampleSize, sampling, split);
			}
			
			else{
				throw new UnsupportedOperationException("No sampling parameter: '" + sampling + "'");
			}
			
			List<String> sample = sampler.getSample();
			System.out.println("\tThe sample contains " + sample.size() + " documents.");

			SerializationHelper.write(sampleFile, sample);
			
		}

	}

	private static String getSampleFile(String directory, String samplingMethod,
			int sampleSize, String sampling_algorithm, int split) {
		
		String data = "Train";
		
		if (directory.contains("Validation")){
			data = "Validation";
		} else if (directory.contains("Test")){
			data = "Test";
		}
		
		return "sample/" + data + "." + samplingMethod +"." + sampleSize + "." + sampling_algorithm + "." + split + ".ser";
		
	}

	private static String getSampleFile(String directory,
			String samplingMethod, String relationship,
			String extractor, int sampleSize, String sampling_algorithm,  int split,
			int docsPerQuerySample, int numQueries) {
		
		String data = "Train";
		
		if (directory.contains("Validation")){
			data = "Validation";
		} else if (directory.contains("Test")){
			data = "Test";
		}
		
		return "sample/" + data + "." + samplingMethod + "." + relationship + "." + extractor + "." + 
			sampleSize + "." + sampling_algorithm + "." + split + "." + docsPerQuerySample + "." + numQueries + ".ser";
		
	}

	
	
	
}
