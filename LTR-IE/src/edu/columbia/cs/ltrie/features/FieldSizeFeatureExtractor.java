package edu.columbia.cs.ltrie.features;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

import pt.utl.ist.online.learning.utils.Pair;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class FieldSizeFeatureExtractor extends FeatureExtractor {
	private static final String LABEL = "SIZE";
	private IndexConnector conn;
	private String field;
	private String label;
	private boolean computeTotal;
	private boolean computeDifferent;
	
	public FieldSizeFeatureExtractor(IndexConnector conn, String field){
		this(conn,field,true,true);
	}
	
	public FieldSizeFeatureExtractor(IndexConnector conn, String field,
			boolean computeTotal, boolean computeDifferent){
		this.conn=conn;
		this.field=field;
		this.label=LABEL + "_" + field;
		this.computeTotal=computeTotal;
		this.computeDifferent=computeDifferent;
	}
	
	public Map<String,Double> extractFeatures(String doc){
		Map<String,Double> d = new HashMap<String, Double>();
		int totalTokens=0;
		int differentTokens=0;
		try {
			Map<String, Integer> frequencies = conn.getTermFrequencies(doc, field);
			differentTokens=frequencies.size();
			for(Entry<String,Integer> entry : frequencies.entrySet()){
				totalTokens+=entry.getValue();
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		if(computeTotal){
			d.put(label + "_TOT", (double) totalTokens);
		}
		if(computeDifferent){
			d.put(label + "_DIF", (double) differentTokens);
		}
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
