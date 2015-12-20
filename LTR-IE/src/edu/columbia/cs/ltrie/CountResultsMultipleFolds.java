package edu.columbia.cs.ltrie;

import java.io.IOException;
import java.util.List;

import com.ibm.avatar.algebra.function.GetInteger;

import edu.columbia.cs.ltrie.excel.ExcelGenerator;
import edu.columbia.cs.ltrie.excel.curves.AveragedCurve;
import edu.columbia.cs.ltrie.excel.curves.BaselineCurve;
import edu.columbia.cs.ltrie.excel.curves.PerfectCurve;
import edu.columbia.cs.ltrie.excel.curves.RankingMethodCurve;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.utils.SerializationHelper;

public class CountResultsMultipleFolds {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		//String initialPath = "/home/goncalo/Desktop/ResultsParial/resultsTest/";
		String initialPath = "/home/goncalo/resultsRank/";
		//String initialPath = "/home/goncalo/resultsRankTest/";
		//String initialPath = "/home/goncalo/Desktop/ResultsParial/resultsRank/";
		//String initialPath = "resultsRank/";
		String[] types = new String[]{"full"};
		String[] relationships = new String[]{"Outbreaks","Indictment-Arrest-Trial", "NaturalDisaster", "ManMadeDisaster","VotingResult","OrgAff","PersonCareer"};
		String[] updateMethods = new String[]{"Shifting"};
		String[] samplings = new String[]{"Explicit"};
		int[] splits = new int[]{1,2,3,4,5};


		for(String type : types){
			for(String relationship : relationships){
				System.out.println("Starting " + relationship);
				for(String sampling : samplings){
					for(String updateMethod : updateMethods){
						getRandomRecallCurve(initialPath, type, sampling, updateMethod, relationship, splits);
						getPerfectRecallCurve(initialPath, type, sampling, updateMethod, relationship, splits);
						getRankSVMInitialCurve(initialPath, type, sampling, updateMethod, relationship, splits);
						getRankSVMAdaptiveCurve(initialPath, type, sampling, updateMethod, relationship, splits);
						getALInitialCurve(initialPath, type, sampling, updateMethod, relationship, splits);
						getALAdaptiveCurve(initialPath, type, sampling, updateMethod, relationship, splits);
					}
					//getFactCrawlInitialRecallCurve(initialPath, type, sampling, relationship, splits);
					//getNewFactCrawlRecallCurve(initialPath, type, sampling, relationship, splits);
				}
			}
		}



		//System.out.println("Updates RankSVM: " + updatesSVM.size());
		//System.out.println("Updates RankAL: " + updatesSVM2.size());

	}

	private static void getPerfectRecallCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			try{
				getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/perfect.data");
			}catch(Exception e){
				System.err.println("Missing " + initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/perfect.data");
			}
		}
	}

	private static void getRandomRecallCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			try{
				getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/baseline.data");
			}catch(Exception e){
				System.err.println("Missing " + initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/baseline.data");
			}
		}
	}

	private static void getFactCrawlRecallCurve(String initialPath, String type, String sampling, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			try{
				getCurve(initialPath+ type +"/" + sampling + "/factcrawl/" + relationship + "/" + split + "/adaptive.data");
			}catch(Exception e){
				System.err.println("Missing " + initialPath+ type +"/" + sampling + "/factcrawl/" + relationship + "/" + split + "/adaptive.data");
			}
		}
	}
	
	private static void getFactCrawlInitialRecallCurve(String initialPath, String type, String sampling, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			try{
				getCurve(initialPath+ type +"/" + sampling + "/factcrawl/" + relationship + "/" + split + "/initial.data");
			}catch(Exception e){
				System.err.println("Missing " + initialPath+ type +"/" + sampling + "/factcrawl/" + relationship + "/" + split + "/initial.data");
			}
		}
	}
	
	private static void getNewFactCrawlRecallCurve(String initialPath, String type, String sampling, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			try{
				getCurve(initialPath+ type +"/" + sampling + "/factcrawl/" + relationship + "/" + split + "/queriesReg.data");
			}catch(Exception e){
				System.err.println("Missing " + initialPath+ type +"/" + sampling + "/factcrawl/" + relationship + "/" + split + "/queriesReg.data");
			}
		}
	}

	private static void getRankSVMInitialCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			try{
				getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/initialRankSVM.data");
			}catch(Exception e){
				System.err.println("Missing " + initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/initialRankSVM.data");
			}
		}
	}

	private static void getRankSVMAdaptiveCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			try{
				getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVM.data");
			}catch(Exception e){
				System.err.println("Missing " + initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVM.data");
			}
		}
	}

	private static void getALInitialCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			try{
				getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/initialActiveLearning.data");
			}catch(Exception e){
				System.err.println("Missing " + initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/initialActiveLearning.data");
			}
		}
	}

	private static void getALAdaptiveCurve(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits) throws IOException, ClassNotFoundException{
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			try{
				getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveActiveLearning.data");
			}catch(Exception e){
				System.err.println("Missing " + initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveActiveLearning.data");
			}
		}
	}

	private static RankingMethodCurve getCurve(String path) throws IOException, ClassNotFoundException{
		return (RankingMethodCurve) SerializationHelper.read(path);
	}

}
