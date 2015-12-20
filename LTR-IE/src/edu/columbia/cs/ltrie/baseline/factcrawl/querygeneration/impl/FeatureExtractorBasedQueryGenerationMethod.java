package edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.QueryGenerationMethod;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.utils.FeaturesRanker;
import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl.utils.ScoresCombinator;
import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.features.FeatureExtractor;

public class FeatureExtractorBasedQueryGenerationMethod extends QueryGenerationMethod{

	private FeatureExtractor featureExtractor;
	private FeaturesRanker<String> featuresRanker;
	private ScoresCombinator<String> featuresCombinator;

	public FeatureExtractorBasedQueryGenerationMethod(FeatureExtractor featureExtractor, FeaturesRanker<String> featuresRanker, ScoresCombinator<String> featuresCombinator, QueryParser qp){
		super(qp);
		this.featureExtractor = featureExtractor;
		this.featuresRanker = featuresRanker;
		this.featuresCombinator = featuresCombinator;
	}

	@Override
	public List<String> generateStringQueries(int numberOfQueries,List<String> relevant, List<String> nonRelevant, Map<String,List<Tuple>> tuples) {
		
		featuresCombinator.initialize();
		
		for (String relevantDoc : relevant) {
			Map<String,Double> features = featureExtractor.extractFeatures(relevantDoc);
			Map<String,Double> correctedFeatures = new HashMap<String, Double>();
			for(Entry<String,Double> entry : features.entrySet()){
				correctedFeatures.put(featureExtractor.getTerm(entry.getKey()).second(), entry.getValue());
			}
			
			featuresCombinator.addFeaturesMap(correctedFeatures);
		}
				
		List<String> ret =  featuresRanker.rankFeatures(featuresCombinator.obtainFeaturesMap());
		return toStringQuery(ret.subList(0, Math.min(numberOfQueries,ret.size())));
		
	}

	private List<String> toStringQuery(List<String> feats) {
		
		List<String> ret = new ArrayList<String>(feats.size());
		
		for (int i = 0; i < feats.size(); i++) {
			ret.add("+\"" + feats.get(i) + "\"");
		}
		
		return ret;
	}
	
	
	
}
