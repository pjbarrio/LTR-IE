package edu.columbia.cs.ltrie;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.ibm.avatar.algebra.function.GetInteger;

import edu.columbia.cs.ltrie.excel.ExcelGenerator;
import edu.columbia.cs.ltrie.excel.curves.AveragedCurve;
import edu.columbia.cs.ltrie.excel.curves.BaselineCurve;
import edu.columbia.cs.ltrie.excel.curves.PerfectCurve;
import edu.columbia.cs.ltrie.excel.curves.RankingMethodCurve;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.excel.curves.TimeCurve;
import edu.columbia.cs.ltrie.utils.SerializationHelper;
import edu.columbia.cs.utils.Pair;

public class ReadResultsTime {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		//String initialPath = "/home/goncalo/Desktop/ResultsParial/resultsRank/";
		//String initialPath = "/home/goncalo/Desktop/ResultsParial/resultsTestSet/resultsRank/";
		//String initialPath = "/home/goncalo/resultsRankTest/";
		String initialPath = "/media/TOSHIBA EXT/resultsRankTest/";
		//String initialPath = "/home/goncalo/Desktop/ResultsParial/resultsTest/";
		//String initialPath = "resultsRank/";
		//String type = "full";
		String type = "full";
		//String relationship = "Indictment-Arrest-Trial";
		String relationship = "OrgAff";
		String updateMethod = "ModelSimilarity";
		//String sampling = "Explicit";
		String sampling = "Smart";
		
		
		double averageTimeRelationshipInSeconds = getAverageTimeRelationshipInSeconds(initialPath, relationship);
		int[] splits = new int[]{4};
		
		ExcelGenerator gen = new ExcelGenerator();
		gen.addRankingCurve("Random_Ranking", getRandomRecallCurve(initialPath, type, sampling, updateMethod, relationship, splits, averageTimeRelationshipInSeconds));
		gen.addRankingCurve("BAgg__IE", getALAdaptiveCurve(initialPath, type, sampling, updateMethod, relationship, splits,averageTimeRelationshipInSeconds));
		gen.addRankingCurve("RSVM__IE", getRankSVMAdaptiveCurve(initialPath, type, sampling, updateMethod, relationship, splits,averageTimeRelationshipInSeconds));
		gen.addRankingCurve("FC", getFactCrawlRecallCurve(initialPath, type, sampling, relationship, splits,averageTimeRelationshipInSeconds));
		gen.addRankingCurve("A__FC", getNewFactCrawlRecallCurve(initialPath, type, sampling, relationship, splits,averageTimeRelationshipInSeconds));
		gen.generateRTime("testTime_" + relationship + "Adaptive",50,false,1,true);
		
		/*ExcelGenerator gen = new ExcelGenerator();
		gen.addRankingCurve("RandomRanking", getRandomRecallCurve(initialPath, type, sampling, updateMethod, relationship, splits));
		gen.addRankingCurve("PerfectRanking", getPerfectRecallCurve(initialPath, type, sampling, updateMethod, relationship, splits));
		//gen.addRankingCurve("InitialRankSVM", getRankSVMInitialCurve(initialPath, type, sampling, updateMethod, relationship, splits));
		gen.addRankingCurve("RankSVM", getRankSVMAdaptiveCurve(initialPath, type, sampling, updateMethod, relationship, splits));
		//gen.addRankingCurve("CyclicRankSVM", getRankSVMAdaptiveCurve(initialPath, type, "Smart", updateMethod, relationship, splits));
		//gen.addRankingCurve("FeatureShifting", getRankSVMAdaptiveCurve(initialPath, type, sampling, "Shifting", relationship, splits));
		//gen.addRankingCurve("TopK", getRankSVMAdaptiveCurve(initialPath, type, sampling, "FeatureRank", relationship, splits));
		//gen.addRankingCurve("FeatureWeight", getRankSVMAdaptiveCurve(initialPath, type, sampling, "ModelSimilarity", relationship, splits));
		//gen.addRankingCurve("RankSVMNoRanking", getRankSVMNoRankingCurve(initialPath, type, sampling, updateMethod, relationship, splits));
		//gen.addRankingCurve("InitialAL", getALInitialCurve(initialPath, type, sampling, updateMethod, relationship, splits));
		gen.addRankingCurve("AL", getALAdaptiveCurve(initialPath, type, sampling, updateMethod, relationship, splits));
		//gen.addRankingCurve("CyclicAL", getALAdaptiveCurve(initialPath, type, "Smart", updateMethod, relationship, splits));
		//gen.addRankingCurve("FactCrawl", getFactCrawlRecallCurve(initialPath, type, sampling, relationship, splits));
		gen.addRankingCurve("AdaptiveFactCrawl", getNewFactCrawlRecallCurve(initialPath, type, sampling, relationship, splits));
		gen.generateR("test_" + relationship + "Adaptive",50,false);
		//gen.generateR("test_" + relationship + "Adaptive",50,true);

		//System.out.println("Updates RankSVM: " + updatesSVM.size());
		//System.out.println("Updates RankAL: " + updatesSVM2.size());
*/
	}

	private static RankingMethodCurve getPerfectRecallCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/perfect.data");
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		return curve;
	}

	private static RankingMethodCurve getRandomRecallCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits, double averageTimeRelationshipInSeconds) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			SortedCurve recallCurveRSVM = (SortedCurve) getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVM.data");
			RankingMethodCurve recallCurve = new BaselineCurve(recallCurveRSVM.getRelevantDocuments().size());
			int sizeCurve = recallCurve.getCurveRetrieval().first().length;
			int realNumberDocs = recallCurveRSVM.getSortedDocuments().size();
			List<Integer> timeCurve = new ArrayList<Integer>();
			for(int k=0; k<sizeCurve-1; k++){
				timeCurve.add(0);
			}
			
			curves[i]=new TimeCurve(recallCurve, timeCurve, averageTimeRelationshipInSeconds*realNumberDocs/sizeCurve);
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		return curve;
	}
	
	private static RankingMethodCurve getFactCrawlRecallCurve(String initialPath, String type, String sampling, String relationship, int[] splits, double averageTimeRelationshipInSeconds) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			RankingMethodCurve recallCurve = getCurve(initialPath+ type +"/" + sampling + "/factcrawl/" + relationship + "/" + split + "/initial.data");
			List<Integer> timeCurve = getTimeCurve(initialPath+ type +"/" + sampling + "/factcrawl/" + relationship + "/" + split + "/initialTimes.Times");
			curves[i]=new TimeCurve(recallCurve, timeCurve, averageTimeRelationshipInSeconds);
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		return curve;
	}
	
	private static RankingMethodCurve getNewFactCrawlRecallCurve(String initialPath, String type, String sampling, String relationship, int[] splits, double averageTimeRelationshipInSeconds) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			RankingMethodCurve recallCurve = getCurve(initialPath+ type +"/" + sampling + "/factcrawl/" + relationship + "/" + split + "/queriesReg.data");
			List<Integer> timeCurve = getTimeCurve(initialPath+ type +"/" + sampling + "/factcrawl/" + relationship + "/" + split + "/times.Times");
			curves[i]=new TimeCurve(recallCurve, timeCurve, averageTimeRelationshipInSeconds);
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		return curve;
	}

	private static RankingMethodCurve getRankSVMInitialCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/initialRankSVM.data");
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		System.out.println("Initial RankSVM: " + curve.getAveragePrecision() + "% �� " + curve.getAveragePrecisionStdDev() + "% of Average Precision");
		System.out.println("Initial RankSVM: " + curve.getRPrecision() + "% �� " + curve.getRPrecisionStdDev() + "% of R-Precision");
		return curve;
	}

	private static RankingMethodCurve getRankSVMAdaptiveCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits, double averageTimeRelationshipInSeconds) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			RankingMethodCurve recallCurve = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVM.data");
			List<Integer> timeCurve = getTimeCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVMTimes.times");
			curves[i]=new TimeCurve(recallCurve, timeCurve, averageTimeRelationshipInSeconds);
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		return curve;
	}
	
	private static RankingMethodCurve getRankSVMNoRankingCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/justQueries.data");
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		System.out.println("No Ranking RankSVM: " + curve.getAveragePrecision() + "% �� " + curve.getAveragePrecisionStdDev() + "% of Average Precision");
		System.out.println("No Ranking RankSVM: " + curve.getRPrecision() + "% �� " + curve.getRPrecisionStdDev() + "% of R-Precision");
		return curve;
	}
	
	private static RankingMethodCurve getALInitialCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/initialActiveLearning.data");
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		System.out.println("Initial Active Learning: " + curve.getAveragePrecision() + "% �� " + curve.getAveragePrecisionStdDev() + "% of Average Precision");
		System.out.println("Initial Active Learning: " + curve.getRPrecision() + "% �� " + curve.getRPrecisionStdDev() + "% of R-Precision");
		return curve;
	}

	private static RankingMethodCurve getALAdaptiveCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits, double averageTimeRelationshipInSeconds) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			RankingMethodCurve recallCurve = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveActiveLearning.data");
			List<Integer> timeCurve = getTimeCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveActiveLearningTimes.times");
			curves[i]=new TimeCurve(recallCurve, timeCurve, averageTimeRelationshipInSeconds);
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		return curve;
	}

	private static RankingMethodCurve getCurve(String path) throws IOException, ClassNotFoundException{
		return (RankingMethodCurve) SerializationHelper.read(path);
	}
	
	private static List<Integer> getTimeCurve(String path) throws IOException, ClassNotFoundException{
		List<Integer> times = (List<Integer>) SerializationHelper.read(path);
		List<Integer> result = new ArrayList<Integer>();
		for(Integer i : times){
			result.add(i/1000);
		}
		return result;
	}

	
	public static double getAverageTimeRelationshipInSeconds(String initialPath, String relationship) throws IOException, ClassNotFoundException{
		File relationshipDirectory = new File(initialPath+"resultsTime/" + relationship);
		double sumTimesInSeconds = 0;
		int numDocuments = 0;
		for(File splitDirectory : relationshipDirectory.listFiles()){
			List<Integer> times = (List<Integer>) SerializationHelper.read(splitDirectory.getAbsolutePath() + "/times.Times");
			int previous = 0;
			for(int i=0; i<times.size(); i++){
				int currentDocumentTime = times.get(i)-previous;
				
				sumTimesInSeconds+=(currentDocumentTime/1000.0);
				numDocuments++;
				
				previous = times.get(i);
			}
		}
		
		double timePerDocumentInSeconds = sumTimesInSeconds/numDocuments;
		
		return timePerDocumentInSeconds;
	}
}
