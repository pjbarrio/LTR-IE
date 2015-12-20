package edu.columbia.cs.ltrie.baseline.factcrawl.strategy.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.utl.ist.online.learning.utils.TimeMeasurer;

import edu.columbia.cs.ltrie.baseline.factcrawl.graph.QMDGraph;
import edu.columbia.cs.ltrie.baseline.factcrawl.graph.scores.FactCrawlQMDScores;
import edu.columbia.cs.ltrie.baseline.factcrawl.graph.scores.QMDScores;
import edu.columbia.cs.ltrie.baseline.factcrawl.strategy.ExtractionStrategy;
import edu.columbia.cs.ltrie.baseline.factcrawl.utils.MapBasedComparator;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class StaticExtractionStrategy extends ExtractionStrategy {
	
	private TimeMeasurer measurer= null;
	
	public StaticExtractionStrategy(IndexConnector index, QMDGraph graph, QMDScores scores) {
		super(index, graph, scores);
	}
	
	public StaticExtractionStrategy(IndexConnector index, QMDGraph graph, QMDScores scores, TimeMeasurer measurer) {
		super(index, graph, scores);
		this.measurer=measurer;
	}

	@Override
	public List<String> sortDocuments(Set<Integer> docs) throws IOException {
		
		Map<Integer, Double> docScores = scores.getDocumentScores();
		System.out.println("Document Scores");
		//System.out.println(docScores.toString());
		System.out.println("Execute with Scores");
		
		List<Integer> bckp = new ArrayList<Integer>(docs);
		
		if (docScores != null && !docScores.isEmpty()){
			Collections.sort(bckp,new MapBasedComparator<Integer>(docScores,false));
		}
		
		List<String> ret = new ArrayList<String>();
		
		for (int i = 0; i < bckp.size(); i++) {
			String doc = index.getPath(bckp.get(i));
			ret.add(new File(doc).getName());
			if(measurer!=null){
				measurer.addCheckPoint();
			}
		}
		
		return ret;
		
	}

}
