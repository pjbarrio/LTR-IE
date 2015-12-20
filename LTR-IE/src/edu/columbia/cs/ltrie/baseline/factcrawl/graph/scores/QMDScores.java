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

public abstract class QMDScores {

	private Set<Query> updatedQueries;
	private Set<QueryGenerationMethod> updatedMethods;
	protected QMDGraph graph;

	public QMDScores(QMDGraph graph){
		this.graph = graph;
	}

	public abstract void addDocumentTuples(Integer docId, int numTup);

	public Map<Integer, Double> getDocumentScores() {

		if (!getUpdatedMethods().isEmpty()){
			notifyUpdatedMethods(getUpdatedMethods());
			getUpdatedMethods().clear();
		}

		if (!getUpdatedQueries().isEmpty()){
			notifyUpdatedQueries(getUpdatedQueries());
			getUpdatedQueries().clear();
		}

		Map<Integer,Double> ret = new MemoryEfficientHashMap<Integer, Double>();

		for (Integer docId : graph.getAllDocs()) {

			ret.put(docId, getDocumentScore(docId));

		}

		return ret;


	}

	protected abstract Double getDocumentScore(Integer docId);

	protected abstract void notifyUpdatedQueries(Set<Query> updatedQueries);

	protected abstract void notifyUpdatedMethods(Set<QueryGenerationMethod> updatedMethods);

	private Set<Query> getUpdatedQueries() {

		if (updatedQueries == null){
			updatedQueries = new MemoryEfficientHashSet<Query>();
		}

		return updatedQueries;
	}

	private Set<QueryGenerationMethod> getUpdatedMethods() {

		if (updatedMethods == null){
			updatedMethods = new MemoryEfficientHashSet<QueryGenerationMethod>();
		}

		return updatedMethods;
	}

	public void updateDocumentTuples(Integer doc, int numTuplesDocument) {

		addDocumentTuples(doc, numTuplesDocument);

		Set<Query> quers = graph.getQueries(doc);

		getUpdatedQueries().addAll(quers);

		for (Query query : quers) {

			getUpdatedMethods().addAll(graph.getQueryGenerationMethods(query));

			graph.addUpdated(doc,query);
			
		}
		
	}

	public abstract Integer getTuples(int docId);

	public void clear() {
		if (updatedQueries != null)
			updatedQueries.clear();
		if (updatedMethods != null)
			updatedMethods.clear();
		_clear();
	}

	protected abstract void _clear();

}
