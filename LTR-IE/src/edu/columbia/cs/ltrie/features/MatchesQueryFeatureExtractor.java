package edu.columbia.cs.ltrie.features;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;

import pt.utl.ist.online.learning.utils.MemoryEfficientHashSet;
import pt.utl.ist.online.learning.utils.Pair;

import edu.columbia.cs.ltrie.datamodel.DocumentWithFields;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class MatchesQueryFeatureExtractor extends FeatureExtractor {
	private final String LABEL;
	private Set<String> interestingDocs = new MemoryEfficientHashSet<String>();
	private Query q;
	
	private final Double ONE = new Double(1.0);
	
	public MatchesQueryFeatureExtractor(IndexConnector conn, Query q) throws IOException{
		LABEL = "MATCHES_QUERY_" + q;
				
		List<Integer> docs = conn.searchWithoutOrder(q);
		for(Integer doc : docs){
			interestingDocs.add(conn.getPath(doc));
		}
		this.q=q;
	}
	
	public final Map<String,Double> extractFeatures(String doc){
		Map<String,Double> d = new HashMap<String, Double>();
		
		if(interestingDocs.contains(doc)){
			d.put(LABEL, ONE);
		}
		
		return d;
	}

	@Override
	public Pair<String, String> getTerm(String term) {
		String[] splitedKey = term.split("_");
		String query = splitedKey[2];
		query = query.substring(1);
		String[] division = query.split(":");
		return new Pair<String, String>(division[0],division[1]);
	}
	
	@Override
	public Query getQuery(String term) {
		return q;
	}
	
	@Override
	public String toString(){
		return q.toString();
	}
}
