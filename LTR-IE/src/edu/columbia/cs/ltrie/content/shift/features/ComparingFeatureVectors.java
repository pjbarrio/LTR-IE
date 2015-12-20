package edu.columbia.cs.ltrie.content.shift.features;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gdata.util.common.base.Pair;

import edu.columbia.cs.ltrie.utils.SerializationHelper;

import weka.core.Attribute;

public class ComparingFeatureVectors {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		List<Map<String,Pair<Integer,Double>>> ranks = (List<Map<String,Pair<Integer,Double>>>)SerializationHelper.read("model/mapcomparingfvfast.ser");
		
		List<List<Pair<String,Double>>> lists = (List<List<Pair<String,Double>>>)SerializationHelper.read("model/comparingfvfast.ser");
		
		int rank = 10;
		
		for (int i = 0; i < ranks.size(); i++) {
			
			for (int j = 0; j < ranks.size(); j++) {
				
				System.out.println(i + "," + j + "," + calculateSpearmansfootrule(ranks.get(i),lists.get(j),ranks.get(j),rank));
				
			}
			
		}
		
		System.out.println(lists.get(0).subList(0, 10).toString());
		System.out.println(lists.get(1).subList(0, 10).toString());
	}

	private static double calculateSpearmansfootrule(Map<String, Pair<Integer, Double>> gold,
			List<Pair<String, Double>> list, Map<String, Pair<Integer, Double>> maplist, int rank) {
		
		double sum = 0;
		
		for (int i = 0; i < list.size() && i < rank; i++) {
			
			Pair<Integer,Double> pair = gold.get(list.get(i).getFirst());
			
			Integer originalRank;
			Double originalWeight;
			
			if (pair != null){
				originalRank = pair.getFirst();
				originalWeight = pair.getSecond();
			}else{
				originalRank = rank+1;
				originalWeight = 0.0;
			}
			
			sum += Math.abs(originalWeight - maplist.get(list.get(i).getFirst()).getSecond());//*Math.abs(originalRank - maplist.get(list.get(i).getFirst()).getFirst());
			
		}
		
		return sum;
	}
	
}
