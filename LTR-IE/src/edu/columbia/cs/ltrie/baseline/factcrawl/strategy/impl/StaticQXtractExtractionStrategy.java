package edu.columbia.cs.ltrie.baseline.factcrawl.strategy.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.columbia.cs.ltrie.baseline.factcrawl.graph.QMDGraph;
import edu.columbia.cs.ltrie.baseline.factcrawl.graph.scores.FactCrawlQMDScores;
import edu.columbia.cs.ltrie.baseline.factcrawl.strategy.ExtractionStrategy;
import edu.columbia.cs.ltrie.baseline.factcrawl.utils.MapBasedComparator;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class StaticQXtractExtractionStrategy extends ExtractionStrategy {
	
	public StaticQXtractExtractionStrategy(IndexConnector index, QMDGraph graph, FactCrawlQMDScores scores) {
		super(index, graph, scores);
	}

	@Override
	public List<String> sortDocuments(Set<Integer> docs) throws IOException {
		
		List<String> ret = new ArrayList<String>();
		
		for (Integer docId : docs) {
			String doc = index.getPath(docId);
			ret.add(new File(doc).getName());
		}
		
		return ret;
		
	}

}
