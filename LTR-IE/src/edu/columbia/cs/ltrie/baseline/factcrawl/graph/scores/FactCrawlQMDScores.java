package edu.columbia.cs.ltrie.baseline.factcrawl.graph.scores;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.Query;

import pt.utl.ist.online.learning.utils.MemoryEfficientHashMap;
import pt.utl.ist.online.learning.utils.MemoryEfficientHashSet;

import edu.columbia.cs.ltrie.baseline.factcrawl.graph.QMDGraph;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.QueryGenerationMethod;

public class FactCrawlQMDScores  extends QMDScores{

	private Map<Integer,Integer> map;
	private int totalTuples;
	private Map<Query, Double> fScoreTable;
	private Map<QueryGenerationMethod, Double> qgmfScoreTable;
	private Set<Integer> relevantDocuments;
	protected double beta;
	
	public FactCrawlQMDScores(double beta, QMDGraph graph) {
		super(graph);
		this.beta = beta;
	}

	protected double getQueryScore(double beta, Query query) {
		
		double score = 0.0;
		
		Set<QueryGenerationMethod> qgms = graph.getQueryGenerationMethods(query);
		
		for (QueryGenerationMethod qgm : qgms) {
			score+=getFScoreAverageByMethod(qgm,graph,beta)*getFScore(beta,query,graph);
		}
		
		return score;
	}

	protected double getFScore(double beta, Query query, QMDGraph graph) {
		
		Double qScore = getFScoreTable().get(query);
		
		if (qScore == null){
			qScore = calculateFScore(beta,query,graph);
			getFScoreTable().put(query,qScore);
		}
		
		return qScore;
	}

	private Double calculateFScore(double beta, Query query, QMDGraph graph) {
		
		double precision = getPrecision(query,graph);
		double recall = getRecall(query,graph);
		
		if (precision == 0.0 && recall == 0.0)
			return 0.0;
		
		return ((beta*beta + 1.0)*(precision*recall))/((beta*beta*precision)+recall);
		
	}

	private double getRecall(Query query, QMDGraph graph) {
		return (getNumberOfTuples(graph.getDocuments(query)))/(double)totalTuples;
	}

	private double getNumberOfTuples(Set<Integer> documents) {
		
		double ret = 0.0;
		
		for (Integer document: documents) {
			
			ret += getTuplesTable().get(document);
			
		}
		
		return ret;
	}

	private double getPrecision(Query query, QMDGraph graph) {
		return ((double)intersection(graph.getDocuments(query),getRelevantDocuments()).size())/((double)getTuplesTable().size());
	}

	private Set<Integer> intersection(Set<Integer> list1,
			Set<Integer> list2) {
		
		Set<Integer> s;
		
		if (list2.size() > list1.size()){
		
			s = new MemoryEfficientHashSet<Integer>(list1);
			
			s.retainAll(list2);
		
		}else{
			
			s = new MemoryEfficientHashSet<Integer>(list2);
			
			s.retainAll(list1);
			
		}
		
		return s;

	}

	private Map<Query, Double> getFScoreTable() {
		if (fScoreTable == null){
			fScoreTable = new MemoryEfficientHashMap<Query, Double>();
		}
		return fScoreTable;
	}

	protected double getFScoreAverageByMethod(
			QueryGenerationMethod queryGenerationMethod, QMDGraph graph,
			double beta) {
		
		Double qgmScore = getQGMFScoreTable().get(queryGenerationMethod);
		
		if (qgmScore == null){
			qgmScore = calculateFScore(beta,queryGenerationMethod,graph);
			getQGMFScoreTable().put(queryGenerationMethod,qgmScore);
		}
		
		return qgmScore;
		
	}

	private Double calculateFScore(double beta,
			QueryGenerationMethod queryGenerationMethod, QMDGraph graph) {
		
		double sum = 0.0;
		
		Set<Query> queries = graph.getQueriesByQueryMethod(queryGenerationMethod);
		
		for (Query query: queries) {
			
			sum += getFScore(beta, query, graph);
			
		}
		
		return sum / (double)queries.size();
	}

	private Map<QueryGenerationMethod, Double> getQGMFScoreTable() {
		if (qgmfScoreTable == null){
			qgmfScoreTable = new MemoryEfficientHashMap<QueryGenerationMethod, Double>();
		}
		return qgmfScoreTable;
	}

	public void addDocumentTuples(Integer docId, int tuples) {
		
		if (tuples > 0 && !getTuplesTable().containsKey(docId)){
			totalTuples+= tuples;
		}
		
		getTuplesTable().put(docId,tuples);
		if (tuples > 0){
			getRelevantDocuments().add(docId);
		}
		
	}

	private Set<Integer> getRelevantDocuments() {
		
		if (relevantDocuments == null){
			relevantDocuments = new MemoryEfficientHashSet<Integer>();
		}
		
		return relevantDocuments;
	}

	private Map<Integer,Integer> getTuplesTable() {
		if (map == null){
			map = new MemoryEfficientHashMap<Integer, Integer>();
		}
		return map;
	}

	

	protected Double getDocumentScore(Integer docId) {
		
		Set<Query> queries = graph.getQueries(docId);
		
		double score = 0.0;
		
		for (Query query: queries) {
			
			score += getQueryScore(beta,query);
			
		}
		
		return score;
		
	}

	@Override
	protected void notifyUpdatedMethods(
			Set<QueryGenerationMethod> updatedMethods) {
		
		qgmfScoreTable.keySet().removeAll(updatedMethods);
		
	}

	@Override
	protected void notifyUpdatedQueries(Set<Query> updatedQueries) {
		fScoreTable.keySet().removeAll(updatedQueries);
	}

	@Override
	public Integer getTuples(int docId) {
		return getTuplesTable().get(docId);
	}

	@Override
	protected void _clear() {
		if (map != null)
			map.clear();
		if (fScoreTable != null)
			fScoreTable.clear();
		if (qgmfScoreTable != null)
			qgmfScoreTable.clear();
		if (relevantDocuments != null)
			relevantDocuments.clear();
		
	}


}
