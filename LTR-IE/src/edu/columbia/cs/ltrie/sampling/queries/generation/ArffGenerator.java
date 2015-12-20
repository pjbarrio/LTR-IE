package edu.columbia.cs.ltrie.sampling.queries.generation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.Ranker;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Utils;

import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.extractor.ExtractionSystem;
import edu.columbia.cs.ltrie.extractor.impl.cimple.CIMPLEExtractionSystem;

public class ArffGenerator {

	static String prefix = "/proj/dbNoBackup/pjbarrio/Dataset/finalTREC/";
	
	private static weka.core.tokenizers.WordTokenizer wt = new weka.core.tokenizers.WordTokenizer();
	
	private static WordValidator wv = new WordValidator();
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		String[] vals = {"aa","ab","ac","ad","af","ae"};
		
		String rel = args[0]; 
		
		int spl = Integer.valueOf(args[1]);
		
		boolean taswe = Boolean.valueOf(args[2]);
		
		String ieExtractionPlan = "CIMPLEExecutionPlans/" + args[0] + "Plain.plan";
		
		ExtractionSystem ieSystem = new CIMPLEExtractionSystem(ieExtractionPlan);
		
		Set<String> stopWords = new HashSet<String>(FileUtils.readLines(new File("stopWords.txt")));
		
		for (int i = spl; i <= spl; i++) {
			
			String uf = "extractionSplits/" + rel + ".ufs." + vals[i];
			
			int a = 1;
			
			while (!new File(uf).exists()){
				uf = "extractionSplits/" + rel + ".ufs." + vals[i-a];
				a++;
			}
			
			String ul = "extractionSplits/" + rel + ".uls." + vals[i];
			
			List<String> ufs = FileUtils.readLines(new File(uf));
			
			Set<Tuple> tuples = new HashSet<Tuple>();
			
			if (taswe){
			
				for (int j = 0; j < ufs.size(); j++) {
					
					if (j % 5 == 0)
						System.out.println(j);
					
					System.err.println(ufs.get(j));
					
					tuples.addAll(ieSystem.extractTuplesFrom(ufs.get(j), CreateExtractionSplits.getContent(new File(prefix,ufs.get(j)))));
				}
				
			}
			
			List<String> uls = FileUtils.readLines(new File(ul));
			
			boolean[] tasw = {taswe};
			
			for (int j = 0; j < tasw.length; j++) {
				
				List<String> queries;
				
				if (tasw[j]){
					
					queries = generateQueries(ufs,uls,tuples,stopWords);
					
				}else{
					
					queries = generateQueries(ufs,uls,new HashSet<Tuple>(),stopWords);
					
				}
				
				FileUtils.writeLines(new File("QUERIES/" + rel + "-" + tasw[j] + "-" + (i+1)), queries);
				
			}
			
		}

	}

	private static List<String> generateQueries(List<String> ufs,
			List<String> uls, Set<Tuple> tuples, Set<String> stopWords) throws Exception {
		
		List<String> queries = new ArrayList<String>();
		
		stopWords.addAll(setTuplesAsStopWords(tuples));
		
		String content = generateArffStructure(uls,ufs,stopWords);
	
		try {
			
			BufferedReader m_sourceReader = new BufferedReader(new StringReader(content));
			
			Instances instances = new Instances(m_sourceReader);
			
			instances.setClassIndex(0); 

			//Remove Freq and InFreq
			
			In_FrequentRemovalFilter ifr = new In_FrequentRemovalFilter();
			
			ifr.setMinFrequencyvalue(0.003);
			ifr.setMaxFrequencyvalue(0.9);
			
			try {
				
				ifr.setInputFormat(instances);
				
				instances = ifr.process(instances);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			AttributeSelection attsel = new AttributeSelection();

			ASEvaluation eval = new ChiSquaredWithYatesCorrectionAttributeEval();

			ASSearch search = new Ranker();

			attsel.setEvaluator(eval);

			attsel.setSearch(search);
			
			attsel.SelectAttributes(instances);

			double[][] indices = attsel.rankedAttributes();
			
			List<String> remaining = new ArrayList<String>();
			
			for (int i = 0; i < indices.length; i++) {

				Attribute att = instances.attribute((int)indices[i][0]);

				double[] values = instances.attributeToDoubleArray((int)indices[i][0]);

				int usefuls = 0;

				int useless = 0;

				for (int ins = 0; ins < values.length; ins++) {

					if (instances.instance(ins).value(att) == 1.0)
						if (instances.instance(ins).classValue() == 1.0)
							usefuls++;
						else
							useless++;

				}

				if (usefuls > useless){ //the attribute separates

					System.err.println(indices[i][1] + "," + att.name());

					queries.add(att.name());

				} else {

					System.err.println("REM!" + indices[i][1] + "," + att.name());

					remaining.add(att.name());

				}

			}
			
			for (int i = 0; i < remaining.size(); i++) {

				queries.add(remaining.get(remaining.size()-1-i));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
			
		return queries;
		
	}

	private static String generateCSV(ArrayList<Integer> auxData) {
		
		String ret = "";
		
		Collections.sort(auxData);
		
		for (Integer integer : auxData) {
			ret = ret + ", " + integer.toString() + " 1";
		}
		
		return ret;
		
	}
	
	private static boolean validForQuerying(String word, Set<String> stopWords) {
		
		if (stopWords.contains(word) || word.trim().isEmpty())
			return false;
		
		char[] vec = word.toCharArray();
		
		for (int i = 0; i < vec.length; i++) {
			
			if (!Character.isLetter(vec[i]))
				return false;
			
		}
		
		return true;
		
	}
	
	private static String generateArffStructure(List<String> useless,
			List<String> useful, Set<String> stopWords) {
		//Write the string out in case of a backup is needed

		String data = "\n\n@DATA\n\n";
		String word;
		Integer index;
		ArrayList<Integer> auxData = new ArrayList<Integer>();
		Hashtable<String, Integer> wordIndex = new Hashtable<String, Integer>();
		ArrayList<String> wordSet = new ArrayList<String>();
		int newIndexOfWord = 1;
		
		int inindex = 0;
		
		List<String> all = new ArrayList<String>(useless);
		
		all.addAll(useful);
		
		for (String document : all) {
			
			inindex++;
			
			if (inindex % 500 == 0)
				System.out.println(inindex + ": out of: " + all.size() + " -> " + document);
			
			String[] words;
			
			words = getWords(CreateExtractionSplits.getContent(new File(prefix,document)));
			
			data = data + "\n{";
			
			if (useful.contains(document)){
				data = data + "0 1";
			}
			else{
				data = data + "0 0";
			}
			
			auxData.clear();
			
			for (int i = 0; i < words.length; i++) {
				
				word = words[i].toLowerCase();
				
				if (!validForQuerying(word,stopWords))
					continue;
				
				index = wordIndex.get(word);
				
				if (index==null){
					index = new Integer(newIndexOfWord++);
					wordIndex.put(word,index);
					wordSet.add(word);
				}
				
				auxData.add(index);
			}
			
			data = data + generateCSV(auxData);
			
			data = data + "}";
			
		}
		
		String header = "@RELATION qxtract\n";
		
		String attributes = "";
		
		attributes = attributes + "\n@ATTRIBUTE class {0,1}";
		
		for (String string : wordSet) {
			
			attributes = attributes + "\n@ATTRIBUTE " + Utils.quote(string) + " {0,1}";

		}
		
		String arffContent = header + attributes + data;
		
		return arffContent;
		//This step is provisional
		
	}
	
	private static List<String> setTuplesAsStopWords(Set<Tuple> tuples) {
		
		List<String> stopWords = new ArrayList<String>();
		
		Set<String> fields;
		
		String[] splitted;
		
		String value;
		
		for (Tuple tuple : tuples) {
			
			fields = tuple.getFieldNames();
			
			for (String field: fields){
				
				value = tuple.getData(field).getValue();
					
				splitted = getWords(value);
					
				for (String string : splitted) {
						
					stopWords.add(string);
						
				}
				
			}
			
		}
		
		return stopWords;
		
	}

	private static String[] getWords(String content){
		
		wt.tokenize(content);
		
		Set<String> words = new HashSet<String>();
		
		while (wt.hasMoreElements()){
			
			String word = ((String)wt.nextElement()).intern();
			
			if (wv.isValid(word)){
				
				if (word.toLowerCase().equals("class"))
					word = "classssalc";
				
				words.add(word.toLowerCase());
			}
		}
		
		return words.toArray(new String[words.size()]);
	
	}
	
}
