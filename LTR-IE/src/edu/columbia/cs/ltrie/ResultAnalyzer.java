package edu.columbia.cs.ltrie;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import edu.columbia.cs.ltrie.excel.ExcelGenerator;
import edu.columbia.cs.ltrie.excel.curves.BaselineCurve;
import edu.columbia.cs.ltrie.excel.curves.PerfectCurve;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.utils.Pair;

public class ResultAnalyzer {
	public static void main(String[] args) throws IOException, RowsExceededException, WriteException {
		String path = "/home/goncalo/Desktop/svmRank/";
		String relationship = "OrgAff";

		String docFile = relationship + "Test.test";
		String predFile = relationship + "Predictions.data";

		FileInputStream fstreamDoc = new FileInputStream(path + docFile);
		DataInputStream inDoc = new DataInputStream(fstreamDoc);
		BufferedReader brDoc = new BufferedReader(new InputStreamReader(inDoc));
		
		FileInputStream fstreamPred = new FileInputStream(path + predFile);
		DataInputStream inPred = new DataInputStream(fstreamPred);
		BufferedReader brPred = new BufferedReader(new InputStreamReader(inPred));
		
		String strLineDoc;
		String strLinePred;
		//Read File Line By Line
		List<RelevanceNode> relevantDocs = new ArrayList<RelevanceNode>();
		Map<String,Integer> relevantDocuments = new HashMap<String, Integer>();
		while ((strLineDoc = brDoc.readLine()) != null)   {
			strLinePred = brPred.readLine();
			double pred = Double.parseDouble(strLinePred);
			String[] arrayFeatures = strLineDoc.split(" ");
			int relevance = Integer.parseInt(arrayFeatures[0]);
			String pathDoc = arrayFeatures[arrayFeatures.length-1];
			if(relevance!=0){
				relevantDocuments.put(pathDoc, relevance);
			}
			RelevanceNode node = new RelevanceNode(relevance, pred, pathDoc);
			relevantDocs.add(node);
		}
		
		Collections.sort(relevantDocs);
		List<String> sortedList = new ArrayList<String>();
		for(RelevanceNode node : relevantDocs){
			sortedList.add(node.path);
		}
		
		int numDocs = sortedList.size();
		ExcelGenerator gen = new ExcelGenerator();
		gen.addRankingCurve("Random Ranking", new BaselineCurve(numDocs));
		gen.addRankingCurve("Perfect Ranking", new PerfectCurve(numDocs, relevantDocuments));
		gen.addRankingCurve("SVMRank", new SortedCurve(numDocs, sortedList, relevantDocuments));
		gen.generateExcel("test" + relationship + "SVMRank.xls");
		
	}

	private static class RelevanceNode implements Comparable<RelevanceNode>{

		private int relevance;
		private double prediction;
		private String path;
		
		public RelevanceNode(int relevance, double prediction, String path){
			this.relevance=relevance;
			this.prediction=prediction;
			this.path=path;
		}
		
		@Override
		public int compareTo(RelevanceNode o) {
			return (int) -Math.signum(prediction-o.prediction);
		}
	}
}
