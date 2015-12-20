package edu.columbia.cs.ltrie;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import pt.utl.ist.online.learning.utils.Statistics;
import pt.utl.ist.online.learning.utils.UpdateStatistics;

import com.ibm.avatar.algebra.function.GetInteger;

import edu.columbia.cs.ltrie.excel.ExcelGenerator;
import edu.columbia.cs.ltrie.excel.curves.AveragedCurve;
import edu.columbia.cs.ltrie.excel.curves.BaselineCurve;
import edu.columbia.cs.ltrie.excel.curves.PerfectCurve;
import edu.columbia.cs.ltrie.excel.curves.RankingMethodCurve;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.utils.SerializationHelper;

public class ReadUpdateStatisticsMultipleFolds {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		String initialPath = "/media/TOSHIBA EXT/resultsRankUpdateStatistics/resultsRank/";
		//String initialPath = "resultsRank/";
		String type = "full";
		//String type = "search";
		//String relationship = "ManMadeDisaster";
		//String relationship = "VotingResult";
		//String relationship = "NaturalDisaster";
		//String relationship = "Indictment-Arrest-Trial";
		//String relationship = "PersonCareer";
		//String relationship = "OrgAff";
		String relationship = "Outbreaks";
		String[] updateMethods = new String[]{"Window", "ModelSimilarity"};
		String sampling = "Explicit";
		//String sampling = "Query";
		int[] splits = new int[]{1,2,3,4,5};
		int numDocs = 669457;
		int numPartitions=10;

		System.out.println("RankSVM");
		Map<Integer,int[]> table = new HashMap<Integer,int[]>();
		for(int partition=0; partition<numPartitions; partition++){
			table.put(partition, new int[updateMethods.length]);
		}
		for(int i=0; i<updateMethods.length; i++){
			String updateMethod = updateMethods[i];
			getRankSVMUpdates(initialPath, type, sampling, updateMethod, relationship, splits,numDocs,numPartitions);
		}

		//ExcelGenerator gen = new ExcelGenerator();
		//gen.addRankingCurve("RandomRanking", getRandomRecallCurve(initialPath, type, sampling, updateMethod, relationship, splits));
		//gen.addRankingCurve("PerfectRanking", getPerfectRecallCurve(initialPath, type, sampling, updateMethod, relationship, splits));
		//gen.addRankingCurve("InitialRankSVM", getRankSVMInitialCurve(initialPath, type, sampling, updateMethod, relationship, splits));
		//gen.addRankingCurve("AdaptiveRankSVM", getRankSVMAdaptiveCurve(initialPath, type, sampling, updateMethod, relationship, splits));
		//gen.addRankingCurve("InitialAL", getALInitialCurve(initialPath, type, sampling, updateMethod, relationship, splits));
		//gen.addRankingCurve("AdaptiveAL", getALAdaptiveCurve(initialPath, type, sampling, updateMethod, relationship, splits));
		//gen.addRankingCurve("FactCrawl", getFactCrawlRecallCurve(initialPath, type, sampling, relationship, splits));
		//gen.generateR("test_" + relationship + "Adaptive",200);

		//System.out.println("Updates RankSVM: " + updatesSVM.size());
		//System.out.println("Updates RankAL: " + updatesSVM2.size());
	}

	private static void getRankSVMUpdates(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits, int numDocs, int numPartitions) throws IOException, ClassNotFoundException{
		Map<Integer, Long> totalNumberNew = new HashMap<Integer, Long>();
		Map<Integer, Long> totalNumberRemoved = new HashMap<Integer, Long>();
		Map<Integer, Long> totalNumberUpdates = new HashMap<Integer, Long>();
		
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			List<Integer> currentUpdates = getUpdates(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVMUpdates.updates");
			List<Statistics> currentUpdateStatistics = getUpdateStatistics(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVMUpdates.updates.statistics");
			
			for (int j = 0; j < currentUpdates.size(); j++) {
				Integer numDoc = currentUpdates.get(j);
				UpdateStatistics statistics = (UpdateStatistics) currentUpdateStatistics.get(j);
				int partition = getPartitionNumber(numDoc, numDocs, numPartitions);
				Long totalNumberNewPartition = totalNumberNew.get(partition);
				if (totalNumberNewPartition == null) {
					totalNumberNewPartition = 0L;
				}
				totalNumberNew.put(partition, totalNumberNewPartition + statistics.getNewFeatures());
				Long totalNumberRemovedPartition = totalNumberRemoved.get(partition);
				if (totalNumberRemovedPartition == null) {
					totalNumberRemovedPartition = 0L;
				}
				totalNumberRemoved.put(partition, totalNumberRemovedPartition + statistics.getRemovedFeatures());
				Long totalNumberUpdatesPartition = totalNumberUpdates.get(partition);
				if (totalNumberUpdatesPartition == null) {
					totalNumberUpdatesPartition = 0L;
				}
				totalNumberUpdates.put(partition, totalNumberUpdatesPartition + 1);
			}
		}
		
		Map<Integer, Double> averageNumberNew = new HashMap<Integer, Double>();
		for (Entry<Integer, Long> entry : totalNumberNew.entrySet()) {
			averageNumberNew.put(entry.getKey(), (double)entry.getValue()/ (double)totalNumberUpdates.get(entry.getKey()));
		}
		
		Map<Integer, Double> averageNumberRemoved = new HashMap<Integer, Double>();
		for (Entry<Integer, Long> entry : totalNumberRemoved.entrySet()) {
			averageNumberRemoved.put(entry.getKey(), (double)entry.getValue()/ (double)totalNumberUpdates.get(entry.getKey()));
		}
		
		System.out.println(averageNumberNew);
		System.out.println(averageNumberRemoved);
	}
	
	private static int getPartitionNumber(int currentDoc, int numDocs, int numPartitions) {
		int currentDocPartition = currentDoc*numPartitions/numDocs;
		return currentDocPartition;		
	}

	private static List<Integer> getUpdates(String path) throws IOException, ClassNotFoundException{
		return (List<Integer>) SerializationHelper.read(path);
	}
	
	private static List<Statistics> getUpdateStatistics(String path) throws IOException, ClassNotFoundException{
		return (List<Statistics>) SerializationHelper.read(path);
	}

}
