package edu.columbia.cs.ltrie;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import com.ibm.avatar.algebra.function.GetInteger;

import edu.columbia.cs.ltrie.excel.ExcelGenerator;
import edu.columbia.cs.ltrie.excel.curves.AveragedCurve;
import edu.columbia.cs.ltrie.excel.curves.BaselineCurve;
import edu.columbia.cs.ltrie.excel.curves.PerfectCurve;
import edu.columbia.cs.ltrie.excel.curves.RankingMethodCurve;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.utils.SerializationHelper;

public class ReadResultsMultipleFolds {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		//String initialPath = "/home/goncalo/Desktop/ResultsParial/resultsRank/";
		//String initialPath = "/home/goncalo/Desktop/ResultsParial/resultsTestSet/resultsRank/";
		//String initialPath = "/home/goncalo/Desktop/resultsRank/resultsRank/";
		//String initialPath = "/home/goncalo/resultsRankTest/";
		//String initialPath = "/media/TOSHIBA EXT/NewResults/";
		String initialPath = "/media/TOSHIBA EXT/resultsRankTest/";
		//String initialPath = "/home/goncalo/Desktop/ResultsParial/resultsTest/";
		//String initialPath = "resultsRank/";
		String type = "full";
		//String type = "search100";
		String[] relationships = new String[]{"Indictment-Arrest-Trial","VotingResult",/*"PersonCareer",*/"OrgAff","Outbreaks","NaturalDisaster","ManMadeDisaster"};
		//String relationship = "VotingResult";
		//String relationship = "Indictment-Arrest-Trial";
		String updateMethod = "ModelSimilarity";
		//String updateMethod = "Window";
		//String sampling = "Explicit";
		String sampling = "Smart";
		int[] splits = new int[]{1,2,3,4,5};

		for(String relationship : relationships){

			System.out.println("Results for " + relationship);
			
			ExcelGenerator gen = new ExcelGenerator();
			gen.addRankingCurve("Random_Ranking", getRandomRecallCurve(initialPath, type, sampling, updateMethod, relationship, splits));
			gen.addRankingCurve("Perfect_Ranking", getPerfectRecallCurve(initialPath, type, sampling, updateMethod, relationship, splits));
			//gen.addRankingCurve("QBC__IE", getALInitialCurve(initialPath, type, sampling, updateMethod, relationship, splits));
			//gen.addRankingCurve("Static_CQS_QBC__IE", getALInitialCurve(initialPath, type, "Smart", updateMethod, relationship, splits));
			gen.addRankingCurve("BAgg__IE", getALAdaptiveCurve(initialPath, type, sampling, updateMethod, relationship, splits));
			//gen.addRankingCurve("Adaptive_CQS_QBC__IE", getALAdaptiveCurve(initialPath, type, "Smart", updateMethod, relationship, splits));
			//gen.addRankingCurve("Base_SRS_RSVM__IE", getRankSVMInitialCurve(initialPath, type, sampling, updateMethod, relationship, splits));
			//gen.addRankingCurve("Base_CQS_RSVM__IE", getRankSVMInitialCurve(initialPath, type, "Smart", updateMethod, relationship, splits));
			gen.addRankingCurve("RSVM__IE", getRankSVMAdaptiveCurve(initialPath, type, sampling, updateMethod, relationship, splits));
			//gen.addRankingCurve("Adaptive_CQS_RSVM__IE", getRankSVMAdaptiveCurve(initialPath, type, "Smart", updateMethod, relationship, splits));
			//gen.addRankingCurve("Feat__S_RSVM__IE", getRankSVMAdaptiveCurve(initialPath, type, sampling, "Shifting", relationship, splits));
			//gen.addRankingCurve("Top__K_RSVM__IE", getRankSVMAdaptiveCurve(initialPath, type, sampling, "FeatureRank", relationship, splits));
			//gen.addRankingCurve("Mod__C_RSVM__IE", getRankSVMAdaptiveCurve(initialPath, type, sampling, "ModelSimilarity", relationship, splits));
			//gen.addRankingCurve("RankSVMNoRanking", getRankSVMNoRankingCurve(initialPath, type, sampling, updateMethod, relationship, splits));
			gen.addRankingCurve("FC", getFactCrawlRecallCurve(initialPath, type, sampling, relationship, splits));
			gen.addRankingCurve("A__FC", getNewFactCrawlRecallCurve(initialPath, type, sampling, relationship, splits));
			gen.generateR("test_" + relationship + "Adaptive",50,false,true);
			//gen.generateR("test_" + relationship + "Adaptive",50,true);

			//System.out.println("Updates RankSVM: " + updatesSVM.size());
			//System.out.println("Updates RankAL: " + updatesSVM2.size());
		}

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

	private static RankingMethodCurve getRandomRecallCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/baseline.data");
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		return curve;
	}

	private static RankingMethodCurve getFactCrawlRecallCurve(String initialPath, String type, String sampling, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/factcrawl/" + relationship + "/" + split + "/initial.data");
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		DecimalFormat decim = new DecimalFormat("#.#");
		System.out.println("FactCrawl: " + decim.format(curve.getAveragePrecision()) + "% �� " + decim.format(curve.getAveragePrecisionStdDev()) + "% of Average Precision");
		System.out.println("FactCrawl: " + decim.format(curve.getRPrecision()) + "% �� " + decim.format(curve.getRPrecisionStdDev()) + "% of R-Precision");
		System.out.println("FactCrawl: " + decim.format(curve.getAreaUnderROC()) + "% �� " + decim.format(curve.getAreaUnderROCStdDev()) + "% of AUC");
		return curve;
	}

	private static RankingMethodCurve getNewFactCrawlRecallCurve(String initialPath, String type, String sampling, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/factcrawl/" + relationship + "/" + split + "/queriesReg.data");
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		DecimalFormat decim = new DecimalFormat("#.#");
		System.out.println("Queries-FactCrawl: " + decim.format(curve.getAveragePrecision()) + "% �� " + decim.format(curve.getAveragePrecisionStdDev()) + "% of Average Precision");
		System.out.println("Queries-FactCrawl: " + decim.format(curve.getRPrecision()) + "% �� " + decim.format(curve.getRPrecisionStdDev()) + "% of R-Precision");
		System.out.println("Queries-FactCrawl:  " + decim.format(curve.getAreaUnderROC()) + "% �� " + decim.format(curve.getAreaUnderROCStdDev()) + "% of AUC");
		return curve;
	}

	private static RankingMethodCurve getRankSVMInitialCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/initialRankSVM.data");
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		DecimalFormat decim = new DecimalFormat("#.#");
		System.out.println("Initial RankSVM: " + decim.format(curve.getAveragePrecision()) + "% �� " + decim.format(curve.getAveragePrecisionStdDev()) + "% of Average Precision");
		System.out.println("Initial RankSVM: " + decim.format(curve.getRPrecision()) + "% �� " + decim.format(curve.getRPrecisionStdDev()) + "% of R-Precision");
		System.out.println("Initial RankSVM: " + decim.format(curve.getAreaUnderROC()) + "% �� " + decim.format(curve.getAreaUnderROCStdDev()) + "% of AUC");
		return curve;
	}

	private static RankingMethodCurve getRankSVMAdaptiveCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVM.data");
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		DecimalFormat decim = new DecimalFormat("#.#");
		System.out.println("Adaptive RankSVM: " + decim.format(curve.getAveragePrecision()) + "% �� " + decim.format(curve.getAveragePrecisionStdDev()) + "% of Average Precision");
		System.out.println("Adaptive RankSVM: " + decim.format(curve.getRPrecision()) + "% �� " + decim.format(curve.getRPrecisionStdDev()) + "% of R-Precision");
		System.out.println("Adaptive RankSVM: " + decim.format(curve.getAreaUnderROC()) + "% �� " + decim.format(curve.getAreaUnderROCStdDev()) + "% of AUC");
		return curve;
	}

	private static RankingMethodCurve getRankSVMNoRankingCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/justQueries.data");
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		DecimalFormat decim = new DecimalFormat("#.#");
		System.out.println("No Ranking RankSVM: " + decim.format(curve.getAveragePrecision()) + "% �� " + decim.format(curve.getAveragePrecisionStdDev()) + "% of Average Precision");
		System.out.println("No Ranking RankSVM: " + decim.format(curve.getRPrecision()) + "% �� " + decim.format(curve.getRPrecisionStdDev()) + "% of R-Precision");
		System.out.println("No Ranking RankSVM: " + decim.format(curve.getAreaUnderROC()) + "% �� " + decim.format(curve.getAreaUnderROCStdDev()) + "% of AUC");
		return curve;
	}

	private static RankingMethodCurve getALInitialCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/initialActiveLearning.data");
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		DecimalFormat decim = new DecimalFormat("#.#");
		System.out.println("Initial Active Learning: " + decim.format(curve.getAveragePrecision()) + "% �� " + decim.format(curve.getAveragePrecisionStdDev()) + "% of Average Precision");
		System.out.println("Initial Active Learning: " + decim.format(curve.getRPrecision()) + "% �� " + decim.format(curve.getRPrecisionStdDev()) + "% of R-Precision");
		System.out.println("Initial Active Learning: " + decim.format(curve.getAreaUnderROC()) + "% �� " + decim.format(curve.getAreaUnderROCStdDev()) + "% of AUC");
		return curve;
	}

	private static RankingMethodCurve getALAdaptiveCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveActiveLearning.data");
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		DecimalFormat decim = new DecimalFormat("#.#");
		System.out.println("Adaptive Active Learning: " + decim.format(curve.getAveragePrecision()) + "% �� " + decim.format(curve.getAveragePrecisionStdDev()) + "% of Average Precision");
		System.out.println("Adaptive Active Learning: " + decim.format(curve.getRPrecision()) + "% �� " + decim.format(curve.getRPrecisionStdDev()) + "% of R-Precision");
		System.out.println("Adaptive Active Learning: " + decim.format(curve.getAreaUnderROC()) + "% �� " + decim.format(curve.getAreaUnderROCStdDev()) + "% of AUC");
		return curve;
	}

	private static RankingMethodCurve getCurve(String path) throws IOException, ClassNotFoundException{
		return (RankingMethodCurve) SerializationHelper.read(path);
	}

}
