package edu.columbia.cs.ltrie;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.avatar.algebra.function.GetInteger;

import edu.columbia.cs.ltrie.excel.ExcelGenerator;
import edu.columbia.cs.ltrie.excel.curves.AveragedCurve;
import edu.columbia.cs.ltrie.excel.curves.BaselineCurve;
import edu.columbia.cs.ltrie.excel.curves.PerfectCurve;
import edu.columbia.cs.ltrie.excel.curves.RankingMethodCurve;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.utils.SerializationHelper;

public class ReadUpdatesMultipleFolds {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		String initialPath = "/home/goncalo/resultsRank/";
		//String initialPath = "resultsRank/";
		String type = "full";
		//String type = "search";
		//String relationship = "ManMadeDisaster";
		String relationship = "VotingResult";
		String[] updateMethods = new String[]{"Window","Shifting","ModelSimilarity","FeatureRank"};
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
			Map<Integer,Integer> histogram = getRankSVMUpdates(initialPath, type, sampling, updateMethod, relationship, splits,numDocs,numPartitions);
			for(Entry<Integer,Integer> entry : histogram.entrySet()){
				int[] partition = table.get(entry.getKey());
				partition[i]=entry.getValue();
			}
		}
		
		System.out.print("Partition");
		for(int i=0; i<updateMethods.length; i++){
			System.out.print("\t" + updateMethods[i]);
		}
		System.out.println();
		for(int partition=0; partition<numPartitions; partition++){
			int[] partitionValues = table.get(partition);
			System.out.print(getPartitionString(partition,numPartitions));
			for(int i=0; i<updateMethods.length; i++){
				System.out.print("\t" + partitionValues[i]);
			}
			System.out.println();
		}
		
		System.out.println("AL");
		table = new HashMap<Integer,int[]>();
		for(int partition=0; partition<numPartitions; partition++){
			table.put(partition, new int[updateMethods.length]);
		}
		for(int i=0; i<updateMethods.length; i++){
			String updateMethod = updateMethods[i];
			Map<Integer,Integer> histogram = getALUpdates(initialPath, type, sampling, updateMethod, relationship, splits,numDocs,numPartitions);
			for(Entry<Integer,Integer> entry : histogram.entrySet()){
				int[] partition = table.get(entry.getKey());
				partition[i]=entry.getValue();
			}
		}
		
		System.out.print("Partition");
		for(int i=0; i<updateMethods.length; i++){
			System.out.print("\t" + updateMethods[i]);
		}
		System.out.println();
		for(int partition=0; partition<numPartitions; partition++){
			int[] partitionValues = table.get(partition);
			System.out.print(getPartitionString(partition,numPartitions));
			for(int i=0; i<updateMethods.length; i++){
				System.out.print("\t" + partitionValues[i]);
			}
			System.out.println();
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
	
	private static String getPartitionString(int partition, int numPartitions){
		double percentagePerPartition = 100.0/numPartitions;
		double start = partition*percentagePerPartition;
		double end = (partition+1)*percentagePerPartition;
		
		DecimalFormat df = new DecimalFormat("00.0");
        String startString = df.format(start);
        String endString = df.format(end);
		return startString + "-" + endString + "%";
	}
	

	private static Map<Integer,Integer> getRankSVMUpdates(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits, int numDocs, int numPartitions) throws IOException, ClassNotFoundException{
		int numDocsPerPartition = numDocs/numPartitions; //Number of documents per partition
		int remainder = numDocs%numPartitions; //The first $remainder partitions will have one document more
		Map<Integer,Integer> partitionMapper = new HashMap<Integer,Integer>();
		int currentPartition=0;
		int currentPartitionSize=0;
		for(int i=0; i<numDocs; i++){
			partitionMapper.put(i, currentPartition);
			currentPartitionSize++;
			
			if((currentPartition<remainder && currentPartitionSize==numDocsPerPartition+1)
			|| (currentPartition>=remainder && currentPartitionSize==numDocsPerPartition)){
				currentPartition++;
				currentPartitionSize=0;
			}
		}
		Map<Integer,Integer> histogram = new HashMap<Integer,Integer>();
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			List<Integer> currentUpdates = getUpdates(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveRankSVMUpdates.updates");
			for(Integer id : currentUpdates){
				Integer partition = partitionMapper.get(id);
				if(partition==null){
					System.out.println(id);
				}
				Integer freq = histogram.get(partition);
				if(freq==null){
					freq=0;
				}
				histogram.put(partition, freq+1);
			}
		}
		
		return histogram;
	}
	
	private static Map<Integer,Integer> getALUpdates(String initialPath, String type, String sampling, String updateMethod, String relationship, int[] splits, int numDocs, int numPartitions) throws IOException, ClassNotFoundException{
		int numDocsPerPartition = numDocs/numPartitions; //Number of documents per partition
		int remainder = numDocs%numPartitions; //The first $remainder partitions will have one document more
		Map<Integer,Integer> partitionMapper = new HashMap<Integer,Integer>();
		int currentPartition=0;
		int currentPartitionSize=0;
		for(int i=0; i<numDocs; i++){
			partitionMapper.put(i, currentPartition);
			currentPartitionSize++;
			
			if((currentPartition<remainder && currentPartitionSize==numDocsPerPartition+1)
			|| (currentPartition>=remainder && currentPartitionSize==numDocsPerPartition)){
				currentPartition++;
				currentPartitionSize=0;
			}
		}
		Map<Integer,Integer> histogram = new HashMap<Integer,Integer>();
		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			List<Integer> currentUpdates = getUpdates(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + split + "/adaptiveActiveLearningUpdates.updates");
			for(Integer id : currentUpdates){
				Integer partition = partitionMapper.get(id);
				if(partition==null){
					System.out.println(id);
				}
				Integer freq = histogram.get(partition);
				if(freq==null){
					freq=0;
				}
				histogram.put(partition, freq+1);
			}
		}
		
		return histogram;
	}

	private static List<Integer> getUpdates(String path) throws IOException, ClassNotFoundException{
		return (List<Integer>) SerializationHelper.read(path);
	}

}
