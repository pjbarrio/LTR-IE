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

public class ReadResultsTimeCreateCSV {

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
		boolean full = Boolean.valueOf(args[6]);
		boolean mmr = Boolean.valueOf(args[7]);



		String type = t + "_" + suf + "-sentence";

		String initialPath = "resultsRank/";


		double averageTimeRelationshipInSeconds = 0.071471466;//getAverageTimeRelationshipInSeconds(initialPath, relationship);

		String updateMethod = args[4]; //"ModelSimilarity","Window", "Explicit", "Smart"

		String sampling = args[5]; //"Query","Smart";

		int[] splits = new int[]{1,2,3,4,5};

		int positiontokeep = 1000;

		System.out.println("Results for " + relationship);

		CSVGenerator gen = new CSVGenerator("LTrain");

		getRankSVMDSAdaptiveCurve(positiontokeep,gen,"RSVM-IE\n(MMR-S)",initialPath, type, sampling, updateMethod, relationship, extractor, splits,averageTimeRelationshipInSeconds);
		getRankSVMSamAdaptiveCurve(positiontokeep,gen,"RSVM-IE\n(Adap-S)", initialPath, type, sampling, updateMethod, relationship, extractor, splits,averageTimeRelationshipInSeconds);
		getBaggDSAdaptiveCurve(positiontokeep,gen,"SVM\n(Sen)",initialPath, type, sampling, "Window", relationship, extractor, splits,averageTimeRelationshipInSeconds);

		
		//			getBaggDSInitialCurve(positiontokeep,gen,"SVM\n(Sen)",initialPath, type, sampling, "Window", relationship, extractor, splits);

		//			getRankSVMAdaptiveCurveMMR(positiontokeep,gen,"RSVM-IE\n(Doc)",initialPath, type, sampling, updateMethod, relationship, extractor, splits);

		
		gen.printTimeCSV(initialPath+ type +"/rsvm." + relationship + "." + extractor + ".");

	}

	private static void getBaggDSAdaptiveCurve(int pos, CSVGenerator gen, String name, String initialPath, String type, String sampling, String updateMethod, String relationship, String extractor, int[] splits, double averageTimeRelationshipInSeconds) throws IOException, ClassNotFoundException{

		String extr = ReadResultsMultipleFoldsSentenceCreateCSV.getExtractor(extractor);

		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			RankingMethodCurve recallCurve= getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveBagg_SamD.data");

			List<Integer> timeCurve = getTimeCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveBaggTimes_SamD.times");

			gen.addTimeCurve(name,ReadResultsMultipleFoldsSentenceCreateCSV.getRelation(relationship,extr),extr,split,slimCurve(pos,new TimeCurve(recallCurve, timeCurve, averageTimeRelationshipInSeconds)));



		}

	}
	
	private static void getRankSVMSamAdaptiveCurve(int pos, CSVGenerator gen, String name, String initialPath, String type, String sampling, String updateMethod, String relationship, String extractor, int[] splits, double averageTimeRelationshipInSeconds) throws IOException, ClassNotFoundException{

		String extr = ReadResultsMultipleFoldsSentenceCreateCSV.getExtractor(extractor);

		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			RankingMethodCurve recallCurve= getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVM_SamD.data");

			List<Integer> timeCurve = getTimeCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVMTimes_SamD.times");

			gen.addTimeCurve(name,ReadResultsMultipleFoldsSentenceCreateCSV.getRelation(relationship,extr),extr,split,slimCurve(pos,new TimeCurve(recallCurve, timeCurve, averageTimeRelationshipInSeconds)));



		}

	}
	

	private static void getRankSVMDSAdaptiveCurve(int pos, CSVGenerator gen, String name, String initialPath, String type, String sampling, String updateMethod, String relationship, String extractor, int[] splits, double averageTimeRelationshipInSeconds) throws IOException, ClassNotFoundException{

		String extr = ReadResultsMultipleFoldsSentenceCreateCSV.getExtractor(extractor);

		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			RankingMethodCurve recallCurve= getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVM_DS.data");

			List<Integer> timeCurve = getTimeCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVMTimes_DS.times");

			gen.addTimeCurve(name,ReadResultsMultipleFoldsSentenceCreateCSV.getRelation(relationship,extr),extr,split,slimCurve(pos,new TimeCurve(recallCurve, timeCurve, averageTimeRelationshipInSeconds)));



		}

	}

	static Pair<double[],double[]> slimCurve(int pos, TimeCurve curve) {

		return curve.getCurveRetrieval();
		
		/*
		Pair<double[], double[]> results = curve.getCurveRetrieval();
		double[] x = results.first();
		double[] y = results.second();

		double[] xs = new double[(int)Math.ceil(x.length / pos)];
		double[] ys = new double[(int)Math.ceil(x.length / pos)];

		int currPos;

		int index=0;

		for (currPos = pos; currPos < x.length; currPos+=pos) {

			xs[index] = x[currPos];
			ys[index] = y[currPos];

			index++;

		}

		return new Pair<double[], double[]>(xs, ys);*/

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
