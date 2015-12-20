package edu.columbia.cs.ltrie.features;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.store.Directory;

import pt.utl.ist.online.learning.utils.Pair;

import edu.columbia.cs.ltrie.datamodel.DocumentWithFields;
import edu.columbia.cs.ltrie.features.similarity.Similarity;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class QuerySimilarityFeatureExtractor extends FeatureExtractor {
	private static final String LABEL = "TF";
	private IndexConnector conn;
	private Similarity similarity;
	private Map<String,Set<String>> desiredFields = new HashMap<String, Set<String>>();

	public QuerySimilarityFeatureExtractor(IndexConnector conn, Similarity similarity, Query q){
		this.conn=conn;
		this.similarity=similarity;
		Set<Term> queryTerms = new HashSet<Term>();
		q.extractTerms(queryTerms);
		
		for(Term t : queryTerms){
			String field = t.field();
			Set<String> fieldValues = desiredFields.get(field);
			if(fieldValues==null){
				fieldValues=new HashSet<String>();
				desiredFields.put(field, fieldValues);
			}
			fieldValues.add(t.text());
		}
	}



	public Map<String,Double> extractFeatures(String doc){
		Map<String,Double> d = new HashMap<String, Double>();
		
		d.put(similarity.getLabel(), similarity.computeSimilarity(desiredFields, doc));

		return d;
	}

	@Override
	public Pair<String,String> getTerm(String term) {
		return null;
	}



	@Override
	public Query getQuery(String term) {
		// TODO Auto-generated method stub
		return null;
	}
}
