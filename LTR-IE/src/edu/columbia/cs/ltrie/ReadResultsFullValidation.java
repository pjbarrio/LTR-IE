package edu.columbia.cs.ltrie;

import java.io.IOException;
import java.util.List;

import edu.columbia.cs.ltrie.excel.ExcelGenerator;
import edu.columbia.cs.ltrie.excel.curves.BaselineCurve;
import edu.columbia.cs.ltrie.excel.curves.PerfectCurve;
import edu.columbia.cs.ltrie.excel.curves.RankingMethodCurve;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.utils.SerializationHelper;

public class ReadResultsFullValidation {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		String initialPath = "/home/goncalo/Desktop/resultsRank/resultsRank/";
		String type = "full";
		String[] relationships = new String[]{"Outbreaks"};
		String updateMethod = "Window";
		String sampling = "Explicit";
		
		for(String relationship : relationships){
			RankingMethodCurve baseline = (RankingMethodCurve) SerializationHelper.read(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/baseline.data");
			RankingMethodCurve perfect = (RankingMethodCurve) SerializationHelper.read(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/perfect.data");
			SortedCurve initialRest = (SortedCurve) SerializationHelper.read(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/initialRankSVM.data");
			initialRest.setName("RankSVM initial");
			SortedCurve adaptiveRest = (SortedCurve) SerializationHelper.read(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/adaptiveRankSVM.data");
			adaptiveRest.setName("RankSVM adaptive Window");
			List<Integer> updatesSVM = (List<Integer>) SerializationHelper.read(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/adaptiveRankSVMUpdates.updates");
			SortedCurve adaptiveRest2 = (SortedCurve) SerializationHelper.read(initialPath+ type +"/" + sampling + "/"+ "ModelSimilarity" + "/" + relationship + "/adaptiveRankSVM.data");
			adaptiveRest2.setName("RankSVM adaptive Similarity");
			List<Integer> updatesSVM2 = (List<Integer>) SerializationHelper.read(initialPath+ type +"/" + sampling + "/"+ "ModelSimilarity" + "/" + relationship + "/adaptiveRankSVMUpdates.updates");
			/*SortedCurve initialRestAL = (SortedCurve) SerializationHelper.read(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/initialActiveLearning.data");
			initialRestAL.setName("Active Learning initial");
			SortedCurve adaptiveRestAL = (SortedCurve) SerializationHelper.read(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/adaptiveActiveLearning.data");
			adaptiveRestAL.setName("Active Learning adaptive");
			List<Integer> updatesAL = (List<Integer>) SerializationHelper.read(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/adaptiveActiveLearningUpdates.updates");*/
			SortedCurve factCrawl = (SortedCurve) SerializationHelper.read(initialPath+ type +"/" + sampling + "/factcrawl/" + relationship + "/adaptive.data");
			factCrawl.setName("FactCrawl");
			
			//RankingMethodCurve activeLearningInitial = (RankingMethodCurve) SerializationHelper.read(initialPath + "activeLearning" + "/full/Window/" + relationship + "/initialALRank1-" + updateMethod +"-Fix-L1-SumSVM.data");
			//RankingMethodCurve activeLearningAdaptive = (RankingMethodCurve) SerializationHelper.read(initialPath + "activeLearning" + "/full/Window/" + relationship + "/adaptiveALRank1-" + updateMethod +"-Fix-L1-SumSVM.data");
			
			
			ExcelGenerator gen = new ExcelGenerator();
			gen.addRankingCurve("RandomRanking", baseline);
			gen.addRankingCurve("PerfectRanking", perfect);
			gen.addRankingCurve("InitialRankSVM", initialRest);
			gen.addRankingCurve("AdaptiveRankSVM", adaptiveRest);
			//gen.addUpdatePoints("AdaptiveRankSVM",updatesSVM);
			//gen.addRankingCurve("InitialAL", initialRestAL);
			gen.addRankingCurve("AdaptiveAL", adaptiveRest2);
			//gen.addUpdatePoints("AdaptiveAL",updatesSVM2);
			gen.addRankingCurve("FactCrawl", factCrawl);
			//gen.addRankingCurve("InitialActive", activeLearningInitial);
			//gen.addRankingCurve("AdaptiveActive", activeLearningAdaptive);
			//gen.generateExcel("test" + relationship + "Adaptive.xls");
			gen.generateR("test_" + relationship + "Adaptive",200,false,true);
			
			System.out.println("Updates RankSVM: " + updatesSVM.size());
			System.out.println("Updates RankAL: " + updatesSVM2.size());
		}
	}

}
