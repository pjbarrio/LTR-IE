package edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.classic.QueryParser;

import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.QueryGenerationMethod;
import edu.columbia.cs.ltrie.datamodel.Tuple;

public class RoundRobinCompositeQGM extends QueryGenerationMethod {

	private List<QueryGenerationMethod> qgms;

	public RoundRobinCompositeQGM(QueryParser qp, List<QueryGenerationMethod> qgms) {
		super(qp);
		this.qgms = qgms;
	}

	@Override
	public List<String> generateStringQueries(int numberOfQueries,List<String> relevant, List<String> nonRelevant, Map<String,List<Tuple>> tuples)
			throws IOException {
		
		int queries = numberOfQueries / qgms.size();
		
		int mod = numberOfQueries % qgms.size();
		
		List<List<String>> lists = new ArrayList<List<String>>();
		
		lists.add(qgms.get(0).generateStringQueries(queries + mod,relevant, nonRelevant, tuples));
		
		for (int i = 1; i < qgms.size(); i++) {
			
			lists.add(qgms.get(i).generateStringQueries(queries,relevant, nonRelevant, tuples));
			
		}
		
		List<String> ret = new ArrayList<String>();
	
		//Round robin.
		
		for (int i = 0; i < queries + mod; i++) {
			for (int j = 0; j < lists.size(); j++) {
				
				if (i < lists.get(j).size()){
					ret.add(lists.get(j).get(i));
				}
			}
		}
		
		return ret;
	}

}
