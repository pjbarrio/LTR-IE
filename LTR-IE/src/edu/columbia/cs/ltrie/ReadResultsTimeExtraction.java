package edu.columbia.cs.ltrie;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import com.google.gdata.data.contacts.Priority;
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

public class ReadResultsTimeExtraction {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		//String initialPath = "/home/goncalo/Desktop/ResultsParial/resultsRank/";
		//String initialPath = "/home/goncalo/Desktop/ResultsParial/resultsTestSet/resultsRank/";
		//String initialPath = "/home/goncalo/resultsRankTest/";
		String initialPath = "";
		//String initialPath = "/home/goncalo/Desktop/ResultsParial/resultsTest/";
		//String initialPath = "resultsRank/";
		String relationship = "ManMadeDisaster";
		String extractor = "Pablo-N-Grams";
		
		System.out.println(getAverageTimeRelationshipInSeconds(initialPath, relationship,extractor));
	}
	
	public static double getAverageTimeRelationshipInSeconds(String initialPath, String relationship, String extractor) throws IOException, ClassNotFoundException{
		File relationshipDirectory = new File(initialPath+"resultsTime/" + relationship);
		double sumTimesInSeconds = 0;
		int numDocuments = 0;
		double size = 2000;
		PriorityQueue<Integer> pq = new PriorityQueue<Integer>();
		
		for(File splitDirectory : relationshipDirectory.listFiles()){
			
			if (!new File(splitDirectory.getAbsolutePath() + "/"+ extractor + "_Train_"+"times.Times").exists())
				continue;
			
			List<Integer> times = (List<Integer>) SerializationHelper.read(splitDirectory.getAbsolutePath() + "/"+ extractor + "_Train_"+"times.Times");
			int previous = 0;
			for(int i=0; i<times.size(); i++){
				int currentDocumentTime = times.get(i)-previous;
				
				pq.add(currentDocumentTime);
				if (pq.size() > size)
					pq.remove();
				
				sumTimesInSeconds+=(currentDocumentTime/1000.0);
				numDocuments++;
				
				previous = times.get(i);
			}
		}
		
		double timePerDocumentInSeconds = sumTimesInSeconds/numDocuments;
		
		double sum = 0.0;
		
		for (Integer integer : pq) {
			sum += integer;
		}
		
		System.out.println((sum/1000.0)/size);
		
		return timePerDocumentInSeconds;
	}

}
