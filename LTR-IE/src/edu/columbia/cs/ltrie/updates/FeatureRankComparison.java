package edu.columbia.cs.ltrie.updates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.ChiSquaredAttributeEval;
import weka.attributeSelection.Ranker;
import weka.core.Attribute;
import weka.core.BinarySparseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import com.google.gdata.util.common.base.Pair;

import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class FeatureRankComparison implements UpdateDecision {

	public static final int SPEARMANS_FOOTRULE = 0;
	public static final int GENERALIZED_SPEARMANS_FOOTRULE = 1;
	private IndexConnector conn;
	private int maxInstances;
	private double threshold;
	private Map<String,Pair<Integer,Double>> featureRank;
	private int numFeatures;
	private Map<String,Integer> termMap;
	private Map<String,Pair<String,int[]>> cachedInstances;
	private int method;
	private double[] PBaris;
	private double[] efficientVector;
	private String[] efficientVectorFeature;
	
	public FeatureRankComparison(IndexConnector conn, int maxInstances, double threshold, List<String> initialDocs, List<String> relevantDocs, int numFeatures, int method) throws Exception{

		this.conn = conn;
		this.maxInstances = maxInstances;
		this.threshold = threshold;
		this.numFeatures = numFeatures;
		termMap = new HashMap<String, Integer>();
		termMap.put("class", 0);
		this.method = method;
		cachedInstances = new HashMap<String,Pair<String,int[]>>();
		this.featureRank = generateFeatureRank(conn,initialDocs,relevantDocs,maxInstances, numFeatures);
		if (this.method == GENERALIZED_SPEARMANS_FOOTRULE)
			generateEfficientVector(featureRank);
//		this.PBaris = generatePBaris(featureRank);
		
		
	}

	private void generateEfficientVector(
			Map<String, Pair<Integer, Double>> featureRank) {
		
		efficientVector = new double[featureRank.size()+1];
		efficientVectorFeature = new String[featureRank.size()+1];
		
		for (Entry<String,Pair<Integer,Double>> pair : featureRank.entrySet()) {
			
			efficientVector[pair.getValue().getFirst()] = pair.getValue().getSecond();
			efficientVectorFeature[pair.getValue().getFirst()] = pair.getKey();
			
		}
		
	}

	private double[] generatePBaris(
			Map<String, Pair<Integer, Double>> featureRank) {
		
		double[] ret = new double[featureRank.size()+1];
		
		double max = -1.0;
		
		for (Entry<String,Pair<Integer,Double>> entry : featureRank.entrySet()) {
			
			double value = entry.getValue().getSecond();
			
			if (value > max)
				max = value;
			
			for (int i = 1; i <= entry.getValue().getFirst(); i++) {
				
				ret[i] += value; 
				
			}
			
		}
		
		for (int i = 1; i < ret.length; i++) {
			
			ret[i] /= max;
			
		}
		
		return ret;
		
	}

	private Map<String, Pair<Integer, Double>> generateFeatureRank(
			IndexConnector conn, List<String> initialDocs,
			List<String> relevantDocs, int maxInstances, int numFeatures) throws Exception {

		

		List<Pair<String,int[]>> instances = new ArrayList<Pair<String,int[]>>();

		//can optimize to only add the ones that are new.
		
		for (int i = Math.max(0, relevantDocs.size()-maxInstances); i < relevantDocs.size(); i++) {

			Pair<String,int[]> inst = cachedInstances.get(relevantDocs.get(i));
			
			if (inst == null){
				inst = new Pair<String,int[]>("1",createInstance(conn.getTermFrequencies(relevantDocs.get(i), NYTDocumentWithFields.BODY_FIELD), termMap));
				cachedInstances.put(relevantDocs.get(i), inst);
			}
			
			instances.add(inst);

		}

		for (int i = Math.max(0, initialDocs.size()-maxInstances); i < initialDocs.size(); i++) {

			Pair<String,int[]> inst = cachedInstances.get(initialDocs.get(i));
			
			if (inst == null){
				inst = new Pair<String,int[]>("0",createInstance(conn.getTermFrequencies(initialDocs.get(i), NYTDocumentWithFields.BODY_FIELD), termMap));
			
				cachedInstances.put(initialDocs.get(i),inst);
			}
			
			instances.add(inst);

		}

		FastVector nomStrings = new FastVector(2);
		for (int j = 0; j <2; j++)
			nomStrings.addElement(Integer.toString(j));

		FastVector fv = new FastVector(termMap.size());

		for (int i = 0; i < termMap.size(); i++) {
			fv.addElement(null);
		}

		for (Entry<String,Integer> pair : termMap.entrySet()) {

			fv.setElementAt(new Attribute(pair.getKey(), nomStrings), pair.getValue());

		}

		Instances dataux = new Instances("relation", fv, instances.size());

		dataux.setClassIndex(0);

		for (Pair<String, int[]> pair : instances) {

			Instance ins = new BinarySparseInstance(1.0, pair.getSecond(), pair.getSecond().length);

			ins.setDataset(dataux);

			ins.setClassValue(pair.getFirst());

			dataux.add(ins);

		}

		return selectAttributes(dataux);

	}

	private static Map<String, Pair<Integer, Double>> selectAttributes(Instances instances) throws Exception {

		AttributeSelection attsel = new AttributeSelection();  // package weka.attributeSelection!

		ASEvaluation eval = new ChiSquaredAttributeEval();
		Ranker search = new Ranker();
		attsel.setEvaluator(eval);
		attsel.setSearch(search);
		attsel.SelectAttributes(instances);
		// obtain the attribute indices that were selected
		double[][] indices = attsel.rankedAttributes();

		Map<String, Pair<Integer, Double>> ret = new HashMap<String, Pair<Integer,Double>>(instances.numAttributes());

		for (int i = 0; i < indices.length; i++) {

			ret.put(instances.attribute((int)indices[i][0]).name(),new Pair<Integer, Double>(i,indices[i][1]));

		}

		return ret;

	}

	private int[] createInstance(Map<String,Integer> termsFreq,
			Map<String, Integer> termMap) {

		Set<Integer> ret = new HashSet<Integer>(); 

		for(String term : termsFreq.keySet()){

			if ("class".equals(term))
				term = "classssalc";
			
			Integer index = termMap.get(term);

			if (index == null){
				index = termMap.size();
				termMap.put(term, index);
			}

			if (index!=null)
				ret.add(index);

		}

		List<Integer> aux = new ArrayList<Integer>(ret);

		Collections.sort(aux);

		int[] arraux = new int[aux.size()];

		for (int i = 0; i < arraux.length; i++) {
			arraux[i] = aux.get(i);
		}

		return arraux;
	}

	@Override
	public boolean doUpdate(List<String> docs, List<String> relevantDocs) {

		Map<String, Pair<Integer, Double>> featRank;
		try {
			
			featRank = generateFeatureRank(conn, docs, relevantDocs, this.maxInstances,numFeatures);
			
			double rankingDiff = calculateRankingDifference(featureRank, featRank,method);
			
			boolean ret = (rankingDiff/featRank.keySet().size())  > threshold;

			if (ret){
				this.featureRank = featRank;
//				PBaris = generatePBaris(this.featureRank);
				if (this.method == GENERALIZED_SPEARMANS_FOOTRULE)
					generateEfficientVector(featureRank);
			}
			return ret;

			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
		
	}

	private double calculateRankingDifference(
			Map<String, Pair<Integer, Double>> featureRank2,
			Map<String, Pair<Integer, Double>> featRank, int method) {
		
		switch (method) {
		case SPEARMANS_FOOTRULE:
			
			return calculateSpearmansfootrule(featureRank, featRank);

		case GENERALIZED_SPEARMANS_FOOTRULE:
			
			return calculateGeneralizedSpearmansfootrule(featureRank, featRank);
		default:
			return -1;
		}

	}

	private double calculateGeneralizedSpearmansfootrule(Map<String, Pair<Integer, Double>> gold,
			Map<String, Pair<Integer, Double>> maplist) {
		
		double sum = 0.0;

		double sumofPbari = 0.0;
		
		for (int i = 1; i < efficientVector.length; i++) {
	
			double Pbari = efficientVector[i];//calculatePBari(entry.getValue().getFirst(),getRanking(entry.getKey(),maplist));
			
			sumofPbari += Pbari;
			
			sum += Pbari*(sumofPbari - calculateDiff(efficientVectorFeature[i], i, maplist));
			
		}

		//System.err.println(sum);
		
		return sum;
		
	}

	

	private double calculateDiff(String feature, int rank, 
			Map<String, Pair<Integer, Double>> maplist) {
		
		//if j in gold < i 
		
		double sum = 0.0;
		
//		for (Entry<String,Pair<Integer,Double>> entry : maplist.entrySet()) {
//			
//			if (entry.getValue().getFirst() <= rank)
//				sum += calculatePBari(entry.getValue().getFirst(),getRanking(entry.getKey(),maplist));
//			
//		}

		int sigmai = getRanking(feature,maplist);
		
		for (int i = 1; i < efficientVectorFeature.length; i++) {
			
			if (getRanking(efficientVectorFeature[i],maplist) < sigmai)
				sum += efficientVector[i];
			
		}
		
		return sum;
		
	}

	private int getRanking(String key,
			Map<String, Pair<Integer, Double>> maplist) {
		
		Pair<Integer, Double> value = maplist.get(key);
		
		if (value == null)
			return maplist.size()+1;
		return value.getFirst();
	}

	private double calculatePBari(int rank, int newRank) {
		
		if (rank == newRank)
			return 1;
		
		return (getp(rank) - getp(newRank)) / ((double)rank - (double)newRank); 
		
	}

	private double getp(int rank) {
		if (rank >= PBaris.length-1)
			return 1;
		return PBaris[rank];
	}

	private double calculateSpearmansfootrule(Map<String, Pair<Integer, Double>> gold,
			Map<String, Pair<Integer, Double>> maplist) {

		//It is actually generalized.
		
		double sum = 0;

		for (Entry<String,Pair<Integer,Double>> entry : maplist.entrySet()) {

			Pair<Integer,Double> pair = gold.get(entry.getKey());

			Integer originalRank;
			Double originalWeight;

			if (pair != null){
				originalRank = pair.getFirst();
				originalWeight = pair.getSecond();
			}else{
				originalRank = gold.size()+1;
				originalWeight = 0.0;
			}

			//The other strategy is to use the Generalized
			
			
			
			sum += Math.abs(originalWeight - entry.getValue().getSecond());//*Math.abs(originalRank - entry.getValue().getFirst());

		}

		//System.err.println(sum);
		
		return sum;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public String report() {
		return "";
	}

}