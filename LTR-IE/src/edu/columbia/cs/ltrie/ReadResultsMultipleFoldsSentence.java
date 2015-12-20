package edu.columbia.cs.ltrie;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import com.ibm.avatar.algebra.function.GetInteger;

import edu.columbia.cs.ltrie.excel.ExcelGenerator;
import edu.columbia.cs.ltrie.excel.curves.AveragedCurve;
import edu.columbia.cs.ltrie.excel.curves.BaselineCurve;
import edu.columbia.cs.ltrie.excel.curves.DiffLengthAveragedCurve;
import edu.columbia.cs.ltrie.excel.curves.PerfectCurve;
import edu.columbia.cs.ltrie.excel.curves.RankingMethodCurve;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.utils.SerializationHelper;

public class ReadResultsMultipleFoldsSentence {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {

		String t = args[0]; //"full"
		String suf = args[1]; //"Train
		String relationship = args[2]; //"Indictment-Arrest-Trial","VotingResult",/*"PersonCareer",*/"OrgAff","Outbreaks","NaturalDisaster","ManMadeDisaster"
		String extractor = args[3];
		
		String type = t + "_" + suf + "-sentence";

		String initialPath = "resultsRank/";

		String updateMethod = args[4]; //"ModelSimilarity","Window", "Explicit", "Smart"

		String sampling = args[5]; //"Query","Smart";

		int[] splits = new int[]{1,2,3,4,5};

		System.out.println("Results for " + relationship);

		ExcelGenerator gen = new ExcelGenerator();
		gen.addRankingCurve("Random_Ranking", getRandomRecallCurve(initialPath, type, sampling, updateMethod, relationship, extractor, splits));
		gen.addRankingCurve("Perfect_Ranking", getPerfectRecallCurve(initialPath, type, sampling, updateMethod, relationship, extractor, splits));
		gen.addRankingCurve("RSVM_IE_SENT", getRankSVMAdaptiveCurve(initialPath, type, sampling, updateMethod, relationship, extractor, splits));
		gen.addRankingCurve("RSVM_IE_DOC", getRankSVMDSAdaptiveCurve(initialPath, type, sampling, updateMethod, relationship, extractor, splits));
		gen.addRankingCurve("RSVM_IE_SAM", getRankSVMSamAdaptiveCurve(initialPath, type, sampling, updateMethod, relationship, extractor, splits));
		gen.generateR(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/Adaptive_Split_avg",50,false,true);

	}

	private static RankingMethodCurve getPerfectRecallCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, String extractor, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/perfect.data");
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		return curve;
	}

	private static RankingMethodCurve getRandomRecallCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, String extractor, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/baseline.data");
		}
		RankingMethodCurve curve = new AveragedCurve(curves);
		return curve;
	}

	private static RankingMethodCurve getRankSVMAdaptiveCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, String extractor, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVM.data");
		}
		RankingMethodCurve curve = new DiffLengthAveragedCurve(curves);
		DecimalFormat decim = new DecimalFormat("#.#");
		System.out.println("Adaptive RankSVM: " + decim.format(curve.getAveragePrecision()) + "% �� " + decim.format(curve.getAveragePrecisionStdDev()) + "% of Average Precision");
		System.out.println("Adaptive RankSVM: " + decim.format(curve.getRPrecision()) + "% �� " + decim.format(curve.getRPrecisionStdDev()) + "% of R-Precision");
		System.err.println("Comment out AUC?");
//		System.out.println("Adaptive RankSVM: " + decim.format(curve.getAreaUnderROC()) + "% �� " + decim.format(curve.getAreaUnderROCStdDev()) + "% of AUC");
		return curve;
	}

	private static RankingMethodCurve getRankSVMDSAdaptiveCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, String extractor, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVM_DS.data");
		}
		RankingMethodCurve curve = new DiffLengthAveragedCurve(curves);
		DecimalFormat decim = new DecimalFormat("#.#");
		System.out.println("Adaptive_DS RankSVM: " + decim.format(curve.getAveragePrecision()) + "% �� " + decim.format(curve.getAveragePrecisionStdDev()) + "% of Average Precision");
		System.out.println("Adaptive_DS RankSVM: " + decim.format(curve.getRPrecision()) + "% �� " + decim.format(curve.getRPrecisionStdDev()) + "% of R-Precision");
		System.err.println("Comment out AUC?");
//		System.out.println("Adaptive_DS RankSVM: " + decim.format(curve.getAreaUnderROC()) + "% �� " + decim.format(curve.getAreaUnderROCStdDev()) + "% of AUC");
		return curve;
	}
	
	private static RankingMethodCurve getRankSVMSamAdaptiveCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, String extractor, int[] splits) throws IOException, ClassNotFoundException{
		RankingMethodCurve[] curves = new RankingMethodCurve[splits.length];
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			curves[i] = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVM_SamD.data");
		}
		RankingMethodCurve curve = new DiffLengthAveragedCurve(curves);
		DecimalFormat decim = new DecimalFormat("#.#");
		System.out.println("Adaptive_SamD RankSVM: " + decim.format(curve.getAveragePrecision()) + "% �� " + decim.format(curve.getAveragePrecisionStdDev()) + "% of Average Precision");
		System.out.println("Adaptive_SamD RankSVM: " + decim.format(curve.getRPrecision()) + "% �� " + decim.format(curve.getRPrecisionStdDev()) + "% of R-Precision");
		System.err.println("Comment out AUC?");
//		System.out.println("Adaptive_SamD RankSVM: " + decim.format(curve.getAreaUnderROC()) + "% �� " + decim.format(curve.getAreaUnderROCStdDev()) + "% of AUC");
		return curve;
	}
	
	private static RankingMethodCurve getCurve(String path) throws IOException, ClassNotFoundException{
		return (RankingMethodCurve) SerializationHelper.read(path);
	}

}
