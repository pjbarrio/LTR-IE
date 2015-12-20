package edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.classic.QueryParser;

import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.QueryGenerationMethod;
import edu.columbia.cs.ltrie.datamodel.Tuple;

public class FactTypeNameQGM extends QueryGenerationMethod {

	public static Map<String,String[]> dictionary = new HashMap<String, String[]>();
	static{
		dictionary.put("OrgAff", new String[]{"person","organization","affiliation"});
		dictionary.put("Outbreaks", new String[]{"disease","outbreaks"});
		dictionary.put("PersonCareer", new String[]{"person","career"});
		dictionary.put("NaturalDisaster", new String[]{"natural","disaster", "location"});
		dictionary.put("ManMadeDisaster", new String[]{"man","made","disaster", "location"});
		dictionary.put("Indictment-Arrest-Trial", new String[]{"person","charge"});
		dictionary.put("VotingResult", new String[]{"election","winner"});
	}
	
	private String[] words;
	
	public FactTypeNameQGM(QueryParser qp, String relationship) {
		super(qp);
		words = dictionary.get(relationship);
		if(words==null){
			words= new String[]{};
		}
		
	}

	@Override
	public List<String> generateStringQueries(int numberOfQueries,List<String> relevant, List<String> nonRelevant, Map<String,List<Tuple>> tuples) {
		
		List<String> ret = new ArrayList<String>(words.length);
		
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < words.length; i++) {
			if(i==0){
				result.append("+" + words[i]);
			}else{
				result.append(" +" + words[i]);
			}
		}
		ret.add(result.toString());
		
		return ret;
	
	}

}
