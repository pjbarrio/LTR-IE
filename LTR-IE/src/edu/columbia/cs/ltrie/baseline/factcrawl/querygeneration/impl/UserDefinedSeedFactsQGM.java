package edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.classic.QueryParser;

import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.QueryGenerationMethod;
import edu.columbia.cs.ltrie.datamodel.Tuple;

public class UserDefinedSeedFactsQGM extends QueryGenerationMethod {

	private List<Tuple> userTuples;

	public UserDefinedSeedFactsQGM(	QueryParser qp,List<Tuple> userTuples) {
		super(qp);
		this.userTuples = userTuples;
	}

	@Override
	public List<String> generateStringQueries(int numberOfQueries,List<String> relevant, List<String> nonRelevant, Map<String,List<Tuple>> tuples) {
		
		List<String> ret = new ArrayList<String>(Math.min(numberOfQueries, userTuples.size()));
		
		for (int i = 0; i < userTuples.size() && i < numberOfQueries; i++) {
			
			ret.add(getStringTuple(userTuples.get(i)));
			
		}
		
		return ret;
	}

	public static String getStringTuple(Tuple tuple) {
		
		String ret = "";
		
		for (String field : tuple.getFieldNames()) {
			
			ret += "+\"" + tuple.getData(field).getValue() + "\"";
			
		}
		
		return ret;
		
	}

}
