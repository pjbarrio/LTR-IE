package edu.columbia.cs.ltrie.baseline.factcrawl.strategy.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.columbia.cs.ltrie.baseline.factcrawl.graph.QMDGraph;
import edu.columbia.cs.ltrie.baseline.factcrawl.graph.scores.FactCrawlQMDScores;
import edu.columbia.cs.ltrie.baseline.factcrawl.graph.scores.QMDScores;
import edu.columbia.cs.ltrie.baseline.factcrawl.strategy.ExtractionStrategy;
import edu.columbia.cs.ltrie.baseline.factcrawl.utils.MapBasedComparator;
import edu.columbia.cs.ltrie.extractor.wrapping.ExtractionWrapper;
import edu.columbia.cs.ltrie.indexing.IndexConnector;
import java.util.Set;

public class AdaptiveExtractionStrategy extends ExtractionStrategy {

	private int updateAfter;
	private ExtractionWrapper extractor;
	private Map<String, Integer> relevance;

	public AdaptiveExtractionStrategy(IndexConnector index, QMDGraph graph, QMDScores scores, int updateAfter, ExtractionWrapper extractor, Map<String, Integer> relevance) {
		super(index,graph,scores);
		this.updateAfter = updateAfter;
		this.extractor = extractor;
		this.relevance = relevance;
	}

	@Override
	public List<String> sortDocuments(Set<Integer> docs) throws IOException {
		
		List<Integer> intDocs = new ArrayList<Integer>(docs);
		
		List<String> ret = new ArrayList<String>(intDocs.size());
		
		Map<Integer, Double> docScores = scores.getDocumentScores();
		//System.out.println("Document Scores");
		//System.out.println(docScores.toString());
		System.out.println("Execute with Scores");
		
		if (docScores != null && !docScores.isEmpty()){
			Collections.sort(intDocs,new MapBasedComparator<Integer>(docScores,false));
		}
		
		int processed = 0;
		
		
		while (!intDocs.isEmpty()){
			
			Integer doc = intDocs.remove(0);
			
			String path = index.getPath(doc);
			
			ret.add(path);
			if (relevance != null && !relevance.isEmpty()){
				
				Integer t = relevance.get(path);
				
				if (t == null)
					t = 0;
				
				scores.updateDocumentTuples(doc, t);
			}else{
				scores.updateDocumentTuples(doc, extractor.getNumTuplesDocument(path));
			}
			
			processed++;
			
			if (processed == updateAfter){
				
				System.out.println("Adapting ... ");
				
				processed = 0;
				
				docScores = scores.getDocumentScores();
				
				if (docScores != null && !docScores.isEmpty()){
					Collections.sort(intDocs,new MapBasedComparator<Integer>(docScores,false));
				}
				System.out.println(intDocs.size() + " known documents to go!");
			}
			
		}
		
		return ret;
		
	}

}
