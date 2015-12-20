package edu.columbia.cs.ltrie;

import java.io.IOException;

import edu.columbia.cs.ltrie.excel.ExcelGenerator;
import edu.columbia.cs.ltrie.excel.curves.BaselineCurve;
import edu.columbia.cs.ltrie.excel.curves.PerfectCurve;
import edu.columbia.cs.ltrie.excel.curves.RankingMethodCurve;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.utils.SerializationHelper;

public class ReadResults {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		String type = "full-sentence";//"fullds";//"search50";//"full";
		String[] relationships = new String[]{"NaturalDisaster"};
		String updateMethod = "ModelSimilarity";
		String sampling = "Smart";
		int split = 1;
		
		for(String relationship : relationships){
			RankingMethodCurve baseline = (RankingMethodCurve) SerializationHelper.read("resultsRank/"+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship+ "/" + split + "/baseline.data");
			RankingMethodCurve perfect = (RankingMethodCurve) SerializationHelper.read("resultsRank/"+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship+ "/" + split + "/perfect.data");
			RankingMethodCurve initialRest = (RankingMethodCurve) SerializationHelper.read("resultsRank/"+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/initialRankSVM.data");
			RankingMethodCurve adaptiveRest = (RankingMethodCurve) SerializationHelper.read("resultsRank/"+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVM.data");
//			RankingMethodCurve factCrawl = (RankingMethodCurve) SerializationHelper.read("resultsRank/"+ type +"/" + sampling + "/factcrawl/" + relationship + "/" + split + "/adaptive.data");
			
			ExcelGenerator gen = new ExcelGenerator();
			gen.addRankingCurve("RandomRanking", baseline);
			gen.addRankingCurve("PerfectRanking", perfect);
			gen.addRankingCurve("InitialRanking", initialRest);
			gen.addRankingCurve("AdaptiveRanking", adaptiveRest);
//			gen.addRankingCurve("FactCrawl", factCrawl);

			//gen.generateExcel("test" + relationship + "Adaptive.xls");
			gen.generateR("test_" + relationship + "Adaptive",1000,false,true);
		}
	}

}
