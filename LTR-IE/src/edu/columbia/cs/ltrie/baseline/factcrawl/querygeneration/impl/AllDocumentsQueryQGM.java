package edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.QueryGenerationMethod;
import edu.columbia.cs.ltrie.datamodel.Tuple;

public class AllDocumentsQueryQGM extends QueryGenerationMethod {

	public AllDocumentsQueryQGM(QueryParser qp) {
		super(qp);
	}
	
	public List<Query> generateQueries(int numberOfQueries,List<String> relevant, List<String> nonRelevant, Map<String,List<Tuple>> tuples) throws ParseException, IOException{
		List<Query> queries = new ArrayList<Query>();
		queries.add(new MatchAllDocsQuery());
		return queries;
	}

	@Override
	public List<String> generateStringQueries(int numberOfQueries,
			List<String> relevant, List<String> nonRelevant,
			Map<String, List<Tuple>> tuples) throws IOException {
		throw new UnsupportedOperationException();
	}

	

}
