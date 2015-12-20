package edu.columbia.cs.ltrie.baseline.factcrawl.strategy;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;

import edu.columbia.cs.ltrie.baseline.factcrawl.graph.QMDGraph;
import edu.columbia.cs.ltrie.baseline.factcrawl.graph.scores.FactCrawlQMDScores;
import edu.columbia.cs.ltrie.baseline.factcrawl.graph.scores.QMDScores;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public abstract class ExtractionStrategy {

	protected IndexConnector index;
	protected QMDGraph graph;
	protected QMDScores scores;

	public ExtractionStrategy (IndexConnector index, QMDGraph graph, QMDScores scores){
		this.index = index;
		this.graph = graph;
		this.scores = scores;
	}
	
	public abstract List<String> sortDocuments(Set<Integer> docs) throws IOException, ParseException;
	
	public List<String> sortDocuments(Set<Integer> docs, QMDScores scores) throws IOException, ParseException {
		this.scores.clear();
		this.scores = scores;
		System.gc();
		return sortDocuments(docs);
	}
	
}
