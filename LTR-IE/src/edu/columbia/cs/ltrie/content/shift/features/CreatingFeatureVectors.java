package edu.columbia.cs.ltrie.content.shift.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gdata.util.common.base.Pair;

import edu.columbia.cs.ltrie.utils.SerializationHelper;


import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.ChiSquaredAttributeEval;
import weka.attributeSelection.GainRatioAttributeEval;
import weka.attributeSelection.Ranker;
import weka.attributeSelection.SVMAttributeEval;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class CreatingFeatureVectors {

	public static void main(String[] args) throws Exception {

		boolean[] tasw = {true,false};
		int[] spl = {1,2,3,4,5};

		List<Instances> list = new ArrayList<Instances>();

		for (int i = 0; i < spl.length; i++) {

			for (int j = 0; j < tasw.length; j++) {

				String file = "/proj/db-files2/NoBackup/pjbarrio/Experiments/Bootstrapping/arffModel/TREC/ManMadeDisaster/"+tasw[j]+"/BONG-ManMadeDisaster-SF-HMM-model_"+spl[i]+"_5000.arff";

				System.out.println("loading..." + file);

				DataSource source = new DataSource(file);
				Instances data = source.getDataSet();
				if (data.classIndex() == -1)
					data.setClassIndex(0);

				list.add(data);

			}

		}

		System.out.println("Loaded.");

		List<List<Pair<String,Double>>> ranks = new ArrayList<List<Pair<String,Double>>>();

		for (int i = 0; i < list.size(); i++) {

			System.out.println("selecting..." + i);

			ranks.add(selectAttributes(list.get(i)));

		}

		System.out.println("Ranked.");
		
		SerializationHelper.write("model/comparingfvfast.ser", ranks);
		
		List<Map<String,Pair<Integer,Double>>> mapRanks = new ArrayList<Map<String,Pair<Integer,Double>>>();
		
		for (int i = 0; i < ranks.size(); i++) {
			
			Map<String,Pair<Integer,Double>> mapRank = new HashMap<String, Pair<Integer,Double>>();
			
			for (int j = 0; j < ranks.get(i).size(); j++){
				
				//mapRank Att, <pos,value>
				
				mapRank.put(ranks.get(i).get(j).getFirst(), new Pair<Integer,Double>(j+1,ranks.get(i).get(j).getSecond()));
				
			}
			
			mapRanks.add(mapRank);
			
		}
		
		SerializationHelper.write("model/mapcomparingfvfast.ser", mapRanks);
		
	}

	private static List<Pair<String,Double>> selectAttributes(Instances instances) throws Exception {

		AttributeSelection attsel = new AttributeSelection();  // package weka.attributeSelection!
//		ASEvaluation eval = new SVMAttributeEval();
		
		ASEvaluation eval = new ChiSquaredAttributeEval();
		Ranker search = new Ranker();
		attsel.setEvaluator(eval);
		attsel.setSearch(search);
		attsel.SelectAttributes(instances);
		// obtain the attribute indices that were selected
		double[][] indices = attsel.rankedAttributes();

		List<Pair<String,Double>> ret = new ArrayList<Pair<String,Double>>(instances.numAttributes());
		
		for (int i = 0; i < indices.length; i++) {

			ret.add(new Pair<String, Double>(instances.attribute((int)indices[i][0]).name(),indices[i][1]));
			
		}

		return ret;
		
	}

}
