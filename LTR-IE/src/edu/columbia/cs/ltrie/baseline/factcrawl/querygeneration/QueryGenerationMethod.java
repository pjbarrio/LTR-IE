package edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import edu.columbia.cs.ltrie.datamodel.Tuple;

public abstract class QueryGenerationMethod {

	private QueryParser qp;

	public QueryGenerationMethod(QueryParser qp){
		this.qp = qp;
	}
	 
	public List<Query> generateQueries(int numberOfQueries,List<String> relevant, List<String> nonRelevant, Map<String,List<Tuple>> tuples) throws ParseException, IOException{
		return transformIntoQueries(generateStringQueries(numberOfQueries,relevant, nonRelevant, tuples));
	}

	private List<Query> transformIntoQueries(List<String> stringQueries) throws ParseException {
		
		List<Query> queries = new ArrayList<Query>(stringQueries.size());
		
		for (int i = 0; i < stringQueries.size(); i++) {
			Query q = qp.parse(stringQueries.get(i));
			queries.add(q);
		}
		
		return queries;
	}

	public abstract List<String> generateStringQueries(int numberOfQueries,List<String> relevant, List<String> nonRelevant, Map<String,List<Tuple>> tuples) throws IOException;

}
