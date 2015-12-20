package edu.columbia.cs.ltrie.sampling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.FromWeightedFileInitialWordLoader;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.InitialWordLoader;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.impl.FromFileInitialWordLoader;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class CyclicInitialQuerySamplingTechnique implements SamplingTechnique {

	private List<String> sample;
	
	public CyclicInitialQuerySamplingTechnique(IndexConnector index, QueryParser qp, String queryFile, int numDocsPerQuery, int numQueries, int maxSize) throws ParseException, IOException{
		
		sample = new ArrayList<String>();
		
		Set<Integer> set = new HashSet<Integer>();
		
		Map<Query, List<Integer>> map = new HashMap<Query,List<Integer>>();
		
		InitialWordLoader iwl = new FromWeightedFileInitialWordLoader(qp,queryFile);
		List<Query> words = iwl.getInitialQueries().subList(0, numQueries);
		boolean hasMore = true;
		
		while(hasMore){
		
			hasMore = false;
			
			for(Query q : words){
				List<Integer> docIds = getDocs(index,map,q);
				for(int i = 0; i < numDocsPerQuery && !docIds.isEmpty(); i++){
					
					Integer val = docIds.remove(0);
					
					if (!set.contains(val)){
						sample.add(index.getPath(val));
						set.add(val);
						if (sample.size() == maxSize)
							return;
					}
	
				}
				
				if (!docIds.isEmpty())
					hasMore = true;
				
			}

		}
	}

	private List<Integer> getDocs(IndexConnector index,
			Map<Query, List<Integer>> map, Query q) throws IOException {
		
		List<Integer> ret = map.get(q);
		
		if (ret == null){
			ret = new ArrayList<Integer>(index.search(q));
			map.put(q, ret);
		}
		
		return ret;
	}

	@Override
	public List<String> getSample() {
		return sample;
	}

}
