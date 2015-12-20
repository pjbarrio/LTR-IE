package edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl;

import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.classic.QueryParser;

import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.QueryGenerationMethod;
import edu.columbia.cs.ltrie.datamodel.Tuple;

public class LexicoSyntacticFactTypeQGM extends QueryGenerationMethod {

	public LexicoSyntacticFactTypeQGM(QueryParser qp) {
		super(qp);
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<String> generateStringQueries(int numberOfQueries,List<String> relevant, List<String> nonRelevant, Map<String,List<Tuple>> tuples) {
		
		System.err.println("Does not apply to us. It uses other relations to find the syntactic patterns");
		
		return null;
	}

}
