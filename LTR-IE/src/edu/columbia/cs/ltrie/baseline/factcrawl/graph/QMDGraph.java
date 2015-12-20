package edu.columbia.cs.ltrie.baseline.factcrawl.graph;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.search.Query;

import pt.utl.ist.online.learning.utils.MemoryEfficientHashMap;
import pt.utl.ist.online.learning.utils.MemoryEfficientHashSet;

import edu.columbia.cs.ltrie.baseline.factcrawl.graph.utils.BiMap;
import edu.columbia.cs.ltrie.baseline.factcrawl.graph.utils.BitBasedBiMap;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.QueryGenerationMethod;

public class QMDGraph implements Iterable<Integer>{

	//	Map<Integer,List<Query>> docQueryTable;
	//	
	//	Map<Query,List<Integer>> queryDocTable;

	BitBasedBiMap<Query> dqTable = new BitBasedBiMap<Query>();
///	BitBasedBiMap<Query> alldqTable = new BitBasedBiMap<Query>();
	BiMap<Query,QueryGenerationMethod> qqTable = new BiMap<Query,QueryGenerationMethod>();
	BitSet docs = new BitSet();
///	BitSet allDocs = new BitSet();
///	Set<Integer> allDocsSorted = new TreeSet<Integer>();
	Map<Integer,Set<Query>> alldocqTable = new MemoryEfficientHashMap<Integer, Set<Query>>();
	
	//	Map<Query,List<QueryGenerationMethod>> queryQGMTable;
	//	
	//	Map<QueryGenerationMethod, List<Query>> QGMQueryTable;

	public void store(Query query, QueryGenerationMethod qgm,
			List<Integer> topDocs, int numberOfDocs) {

		if (qqTable.getByFirst(query).contains(qgm)) //Same query generated twice.
			return;

		qqTable.add(query,qgm);
		for (int i = 0; i < topDocs.size(); i++) {
			Integer docId = topDocs.get(i);
///			if (!allDocs.get(docId)){
///				allDocs.set(docId);
///				allDocsSorted.add(docId);
///			}

///			alldqTable.add(docId,query);

			Set<Query> s = alldocqTable.get(docId);
			
			if (s == null){
				s = new MemoryEfficientHashSet<Query>();
				alldocqTable.put(docId, s);
			}
			
			s.add(query);
			
			if (i<numberOfDocs){
				docs.set(docId);
				dqTable.add(docId,query);
			}
		}
	}

///	public Set<Integer> matchedDocuments(Query query) {
///		return alldqTable.getBySecond(query);
///	}


	@Override
	public Iterator<Integer> iterator() {
		List<Integer> iteratorList = new ArrayList<Integer>(docs.size());
		for (int i = docs.nextSetBit(0); i >= 0; i = docs.nextSetBit(i+1)) {
			iteratorList.add(i);
		}
		return iteratorList.iterator();
	}


	public Set<QueryGenerationMethod> getQueryGenerationMethods(Query query) {
		return qqTable.getByFirst(query);
	}


	public Set<Query> getQueriesByQueryMethod(
			QueryGenerationMethod queryGenerationMethod) {
		return qqTable.getBySecond(queryGenerationMethod);
	}


	public Set<Integer> getDocuments(Query query) {

		return dqTable.getBySecond(query);

	}

	public Set<Integer> getAllDocs() {
		return alldocqTable.keySet();
	}

///	public Set<Query> getQueries(Integer doc) {
///		return alldqTable.getByFirst(doc);
///	}

	public Set<Query> getQueries(Integer doc) {
		return alldocqTable.get(doc);
	}
	
	public void addUpdated(Integer doc, Query query) {
		dqTable.add(doc, query); 		
	}

	public int getPosition(Query query, QueryGenerationMethod qgm) {
		return qqTable.getPosition(query,qgm);
	}

	public int getTotalQueries(QueryGenerationMethod qgm) {
		return qqTable.getBySecond(qgm).size();
	}

///	public int getTotalDocuments(Query query) {
///		return alldqTable.getBySecond(query).size();
///	}

///	public int getPosition(Integer docId, Query query) {
///		return alldqTable.getPosition(docId,query);
///	}




}
