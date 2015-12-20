package edu.columbia.cs.ltrie;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.netlib.util.doubleW;

import com.ibm.avatar.algebra.function.GetInteger;

import edu.columbia.cs.ltrie.excel.ExcelGenerator;
import edu.columbia.cs.ltrie.excel.curves.AveragedCurve;
import edu.columbia.cs.ltrie.excel.curves.BaselineCurve;
import edu.columbia.cs.ltrie.excel.curves.PerfectCurve;
import edu.columbia.cs.ltrie.excel.curves.RankingMethodCurve;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.excel.curves.SpecificCurve;
import edu.columbia.cs.ltrie.excel.curves.TimeCurve;
import edu.columbia.cs.ltrie.utils.SerializationHelper;
import edu.columbia.cs.utils.Pair;

public class ReadResultsTimeScalability {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		//String initialPath = "/home/goncalo/Desktop/ResultsParial/resultsRank/";
		//String initialPath = "/home/goncalo/Desktop/ResultsParial/resultsTestSet/resultsRank/";
		//String initialPath = "/home/goncalo/resultsRankTest/";
		String initialPath = "/media/TOSHIBA EXT/resultsPartialDatasets/";
		//String initialPath = "/home/goncalo/Desktop/ResultsParial/resultsTest/";
		//String initialPath = "resultsRank/";
		//String type = "full";
		String type = "full";
		//String relationship = "Outbreaks";
		String relationship = "Indictment-Arrest-Trial";
		//String relationship = "VotingResult";
		String updateMethod = "ModelSimilarity";
		//String sampling = "Explicit";
		String sampling = "Smart";
		
		int[] values = new int[]{10,20,30,40,50,60,70,80,90,100};
		
		String folder = "resultsRank";
		
		
		double averageTimeRelationshipInSeconds = 0.0;
		int[] splits = new int[]{1,2,3,4,5};
		
		ExcelGenerator gen = new ExcelGenerator();
		gen.addRankingCurve("BAgg__IE", getALAdaptiveCurve(initialPath, values, folder, type, sampling, updateMethod, relationship, splits,averageTimeRelationshipInSeconds));
		gen.addRankingCurve("RSVM__IE", getRankSVMAdaptiveCurve(initialPath, values, folder, type, sampling, updateMethod, relationship, splits,averageTimeRelationshipInSeconds));
		gen.generateRTimeScalability("testTimeScalability_" + relationship + "Adaptive",50,false,1,true);
		
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

	private static RankingMethodCurve getRankSVMAdaptiveCurve(String initialPath, int[] values, String folder, String type, String sampling, String updateMethod, String relationship, int[] splits, double averageTimeRelationshipInSeconds) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			double[] x = new double[values.length+1];
			double[] y = new double[values.length+1];
			x[0] = 0;
			y[0] = 0;
			for(int j=0; j<values.length; j++) {
				int value = values[j];
				RankingMethodCurve recallCurve = getCurve(initialPath + value + "/" + folder + "/" + type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVM.data");
				List<Integer> timeCurve = getTimeCurve(initialPath + value + "/" + folder + "/" + type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVMTimes.times");
				TimeCurve t =new TimeCurve(recallCurve, timeCurve, averageTimeRelationshipInSeconds);
				double[] yValues = t.getCurveRetrieval().second();
				x[j+1] = value;
				y[j+1] = yValues[yValues.length-1];
			}
			
			curves[i]=new SpecificCurve(x, y);
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		return curve;
	}
	
	private static RankingMethodCurve getALAdaptiveCurve(String initialPath, int[] values, String folder, String type, String sampling, String updateMethod, String relationship, int[] splits, double averageTimeRelationshipInSeconds) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			double[] x = new double[values.length+1];
			double[] y = new double[values.length+1];
			x[0] = 0;
			y[0] = 0;
			for(int j=0; j<values.length; j++) {
				int value = values[j];
				RankingMethodCurve recallCurve = getCurve(initialPath + value + "/" + folder + "/" + type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveActiveLearning.data");
				List<Integer> timeCurve = getTimeCurve(initialPath + value + "/" + folder + "/" + type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveActiveLearningTimes.times");
				TimeCurve t =new TimeCurve(recallCurve, timeCurve, averageTimeRelationshipInSeconds);
				double[] yValues = t.getCurveRetrieval().second();
				x[j+1] = value;
				y[j+1] = yValues[yValues.length-1];
			}
			
			curves[i]=new SpecificCurve(x, y);
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
