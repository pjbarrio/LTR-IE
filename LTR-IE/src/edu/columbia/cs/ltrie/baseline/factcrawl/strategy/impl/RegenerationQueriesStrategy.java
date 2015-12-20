package edu.columbia.cs.ltrie.baseline.factcrawl.strategy.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;

import pt.utl.ist.online.learning.utils.TimeMeasurer;

import edu.columbia.cs.ltrie.baseline.factcrawl.RFACT;
import edu.columbia.cs.ltrie.baseline.factcrawl.graph.QMDGraph;
import edu.columbia.cs.ltrie.baseline.factcrawl.graph.scores.FactCrawlQMDScores;
import edu.columbia.cs.ltrie.baseline.factcrawl.graph.scores.QMDScores;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.QueryGenerationMethod;
import edu.columbia.cs.ltrie.baseline.factcrawl.strategy.ExtractionStrategy;
import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.extractor.wrapping.ExtractionWrapper;
import edu.columbia.cs.ltrie.indexing.IndexConnector;
import edu.columbia.cs.ltrie.updates.UpdateDecision;
import edu.columbia.cs.ltrie.updates.UpdatePrediction;
import java.util.Set;


public class RegenerationQueriesStrategy extends ExtractionStrategy {

	private UpdateDecision updateDecision;
	private UpdatePrediction updatePrediction;
	private ExtractionStrategy extractionStrategy;
	private Map<String, Integer> relevance;
	private RFACT factCrawl;
	private List<QueryGenerationMethod> qgm;
	private ExtractionWrapper extractWrapper;
	private int numberOfQueries;
	private int numberOfRetrievedDocsInSample;
	private int docsPerQueryLimit;
	private double betaScore;
	private int sampleSize;
	private Set<String> originalSample;
	private TimeMeasurer measurer;
	
	public RegenerationQueriesStrategy(Map<String,Integer> relevance, IndexConnector index, QMDGraph graph,
			QMDScores scores, UpdateDecision updateDecision, UpdatePrediction updatePrediction, ExtractionStrategy extractionStrategy, RFACT factCrawl, List<QueryGenerationMethod> qgm, ExtractionWrapper extractWrapper, int numberOfQueries, int numberOfRetrievedDocsInSample, int docsPerQueryLimit, double betaScore, int sampleSize, Set<String> originalSample, TimeMeasurer measurer) {
		super(index, graph, scores);
		this.updateDecision = updateDecision;
		this.updatePrediction = updatePrediction;
		this.extractionStrategy = extractionStrategy;
		this.relevance = relevance;
		this.factCrawl = factCrawl;
		this.qgm = qgm;
		this.extractWrapper = extractWrapper;
		this.numberOfQueries = numberOfQueries;
		this.numberOfRetrievedDocsInSample = numberOfRetrievedDocsInSample;
		this.docsPerQueryLimit = docsPerQueryLimit;
		this.betaScore = betaScore;
		this.sampleSize = sampleSize;
		this.originalSample = originalSample;
		this.measurer=measurer;
	}

	@Override
	public List<String> sortDocuments(Set<Integer> docs) throws IOException, ParseException {
		System.out.println(docs.size());
		
		List<String> finalRanking = new ArrayList<String>(docs.size());
		
		List<String> currentBatchDocs = new ArrayList<String>();
		
		List<String> currentBatchRelDocs = new ArrayList<String>();
		
		List<String> currentBatchNotRelDocs = new ArrayList<String>();
		
		List<String> rankedCollection = extractionStrategy.sortDocuments(docs);
		
		boolean detectedUpdate = false;
		
		Set<Integer> processedDocs = new HashSet<Integer>();
		
		while (!rankedCollection.isEmpty()){
			String doc = rankedCollection.remove(0);
		
			processedDocs.add(index.getDocId(doc));
			
			if (originalSample.contains(doc))
				continue;
			
			finalRanking.add(doc);
			
			measurer.addCheckPoint();
			
			currentBatchDocs.add(doc);
			
			if (relevance.containsKey(doc)){
				currentBatchRelDocs.add(doc);
			}else{
				currentBatchNotRelDocs.add(doc);
			}
			
			if(detectedUpdate || ((updateDecision != null && updateDecision.doUpdate(currentBatchDocs, currentBatchRelDocs)) || (updatePrediction != null && updatePrediction.predictUpdate(rankedCollection,0)))){
				
				detectedUpdate = true;
				
				if (currentBatchRelDocs.size() > 0 && currentBatchDocs.size() >= sampleSize){
				
					System.err.println("Updating the queries!");
										
					Set<String> sample = generateSample(currentBatchRelDocs,currentBatchNotRelDocs);
					
					List<String> currentBatchRelDocsSample = new ArrayList<String>();
					
					List<String> currentBatchNotRelDocsSample = new ArrayList<String>();
					
					Map<String,List<Tuple>> tuples = new HashMap<String, List<Tuple>>();
					
					for(String docS : sample){
						List<Tuple> tup = extractWrapper.getTuplesDocument(docS);
						if (tup.isEmpty()){
							currentBatchNotRelDocsSample.add(docS);
						}else{
							currentBatchRelDocsSample.add(docS);
							tuples.put(docS, tup);
						}
					}
					
					System.out.println("Generate Queries With Feature Selection Methods");
					
					graph = factCrawl.evaluateQueryGenerationMethods(qgm,numberOfQueries,numberOfRetrievedDocsInSample, docsPerQueryLimit, currentBatchRelDocsSample, currentBatchNotRelDocsSample, tuples,new HashSet<String>(sample),this.graph);
					
					System.out.println("Extract Crawled Documents");
					
					QMDScores scores = new FactCrawlQMDScores(betaScore,graph);
					
					factCrawl.extractDocuments(graph,scores);
					
					if (updateDecision != null)
						updateDecision.reset();
					if (updatePrediction != null){
						updatePrediction.performUpdate(currentBatchDocs, currentBatchRelDocs);
					}
					
					currentBatchDocs = new ArrayList<String>();
					currentBatchRelDocs = new ArrayList<String>();
					currentBatchNotRelDocs = new ArrayList<String>();
					
					detectedUpdate = false; //has to go back to initial
					
					Set<Integer> dds = graph.getAllDocs();
					
					dds.removeAll(processedDocs);
					
					rankedCollection = extractionStrategy.sortDocuments(dds,scores);
				
					System.err.println("Need to go through: " + dds.size() + " documents");
					
				}
				
			}
			
		}
		
		return finalRanking;
		
	}

	private Set<String> generateSample(List<String> currentBatchRelDocs,
			List<String> currentBatchNotRelDocs) {
		
		Set<String> sample = new HashSet<String>();
		
		List<String> copRel = new ArrayList<String>(currentBatchRelDocs);
		List<String> copNotRel = new ArrayList<String>(currentBatchNotRelDocs);
		
		Collections.shuffle(copRel);
		Collections.shuffle(copNotRel);
		
		for (int i = 0; sample.size() < sampleSize; i++) {
			
			if (copRel.size() > i){
				sample.add(copRel.get(i));
			}
			
			if (copNotRel.size() > i){
				sample.add(copNotRel.get(i));
			}
			
		}
		
		return sample;
		
	}

}
