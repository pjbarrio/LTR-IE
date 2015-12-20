package edu.columbia.cs.ltrie;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import pt.utl.ist.online.learning.utils.Pair;

import edu.columbia.cs.ltrie.active.learning.ActiveLearningOnlineModel;
import edu.columbia.cs.ltrie.features.FeaturesCoordinator;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class CyclicDocumentRetrieval {
	
	private int numQueriesPerRetrieval = 100;
	private int numDocumentsUpdate = 10;
	private Map<Query,List<Integer>> docsPerQuery = new HashMap<Query,List<Integer>>();
	private Map<Query,Integer> currentDocQuery = new HashMap<Query, Integer>();
	private IndexConnector conn;
	private BitSet retrievedDocs = new BitSet();
	private int limit;
	
	public CyclicDocumentRetrieval(IndexConnector conn, List<String> sample) throws IOException, ParseException{
		this(conn,sample,Integer.MAX_VALUE);
	}
	
	public CyclicDocumentRetrieval(IndexConnector conn, List<String> sample, int limit) throws IOException, ParseException{
		this.conn=conn;
		for(String path : sample){
			retrievedDocs.set(conn.getDocId(path));
		}
		this.limit=limit;
	}
	
	public List<String> getMoreDocuments(Map<Pair<String,String>,Double> termWeights) throws IOException{
		final Map<Pair<String,String>,Double> scores = termWeights;
		List<Pair<String,String>> queries = new ArrayList<Pair<String,String>>(termWeights.keySet());
		Collections.sort(queries, new Comparator<Pair<String,String>>() {
			@Override
			public int compare(Pair<String, String> o1, Pair<String, String> o2) {
				return (int) Math.signum(scores.get(o2)-scores.get(o1));
			}
		});
		
		int maxNumQueries = queries.size();
		
		List<String> docs = new ArrayList<String>();
		int skipped = 0;
		for(int i=0; i<Math.min(numQueriesPerRetrieval+skipped,maxNumQueries); i++){
			Pair<String,String> pair = queries.get(i);
			Query query = new TermQuery(new Term(pair.first(), pair.second()));
			List<Integer> docsQuery = docsPerQuery.get(query);
			if(docsQuery==null){
				docsQuery=conn.search(query,limit);
				docsPerQuery.put(query, docsQuery);
				currentDocQuery.put(query, 0);
			}
			int currentDocForQuery = currentDocQuery.get(query);
			if(currentDocForQuery==docsQuery.size()){
				//We submitted all documents for this query - let's skip and consider a new query
				skipped++;
				continue;
			}
			
			int sizeDocsQuery = docsQuery.size();
			int j=currentDocForQuery;
			for(; j<Math.min(currentDocForQuery+numDocumentsUpdate, sizeDocsQuery); j++){
				Integer doc = docsQuery.get(j);
				if(!retrievedDocs.get(doc)){
					String path = conn.getPath(doc);
					docs.add(path);
					retrievedDocs.set(doc);
				}
			}
			currentDocQuery.put(query, j);
			
		}
		
		return docs;
	}
	
	public List<String> getRemainingDocuments() throws IOException{
		Query q = new MatchAllDocsQuery();
		List<Integer> remainingDocs = conn.search(q);
		List<String> result = new ArrayList<String>();
		for(Integer doc : remainingDocs){
			if(!retrievedDocs.get(doc)){
				String path = conn.getPath(doc);
				result.add(path);
			}
		}
		return result;
	}
}
