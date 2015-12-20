package edu.columbia.cs.ltrie.features.similarity;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;

import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class BooleanAndSimilarity implements Similarity{

	private final String LABEL;
	private IndexConnector conn;
	private String field;

	public BooleanAndSimilarity(IndexConnector conn, String field){
		this.conn=conn;
		this.field=field;
		LABEL = "NUM_QUERY_TERMS_SIMILARITY_"+field;
	}

	@Override
	public double computeSimilarity(Map<String, Set<String>> queryTerms,
			String doc) {

		try {
			Set<String> desiredTerms = new HashSet<String>(queryTerms.get(field));
			Set<String> terms = conn.getTermFrequencies(doc, field).keySet();
			int before = desiredTerms.size();
			desiredTerms.retainAll(terms);
			int after = desiredTerms.size();
			return before==after ? 1 : 0;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getLabel() {
		return LABEL;
	}

}
