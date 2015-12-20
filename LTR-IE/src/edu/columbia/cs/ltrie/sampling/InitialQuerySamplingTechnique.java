package edu.columbia.cs.ltrie.sampling;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import pt.utl.ist.online.learning.utils.Pair;

import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.InitialWordLoader;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.impl.FromFileInitialWordLoader;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class InitialQuerySamplingTechnique implements SamplingTechnique {
	private List<String> sample;
	
	public InitialQuerySamplingTechnique(IndexConnector index, QueryParser qp, String queryFile, int numDocsPerQuery,int sampleSize) throws ParseException, IOException{
		
		sample = new ArrayList<String>();
		InitialWordLoader iwl = new FromFileInitialWordLoader(qp,queryFile);
		List<Query> words = iwl.getInitialQueries();
		Map<String,Integer> documents = new HashMap<String,Integer>(); 
		for(Query q : words){
			List<Integer> docIds = index.search(q,numDocsPerQuery);
			for(Integer docId : docIds){
				//sample.add(index.getPath(docId));
				String path = index.getPath(docId);
				Integer freq = documents.get(path);
				if(freq==null){
					freq = 0;
					sample.add(path);
				}
				documents.put(path, freq+1);
			}
		}
		final Map<String,Integer> scores = documents;
		Collections.sort(sample, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return (int) Math.signum(scores.get(o2)-scores.get(o1));
			}
		});
		
		sample=sample.subList(0, sampleSize);
	}

	@Override
	public List<String> getSample() {
		return sample;
	}

}
