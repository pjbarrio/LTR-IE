package edu.columbia.cs.ltrie.features;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

import edu.columbia.cs.ltrie.active.learning.classifier.util.FeaturesPipeline;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.indexing.IndexConnector;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import pt.utl.ist.online.learning.utils.Pair;

public class TextBasedPOSFeatureExtractor extends FeatureExtractor {

	private IndexConnector conn;
	private Set<String> fields;
	private StanfordCoreNLP pipeline;
	private boolean freq;

	public TextBasedPOSFeatureExtractor(IndexConnector conn, boolean freq, String... fields) {
		this.conn = conn;
		
		this.fields = new HashSet<String>(fields.length);
		
		for (int i = 0; i < fields.length; i++) {
			this.fields.add(fields[i]);
		}
		
		Properties props = new Properties();
	    props.put("annotators","tokenize, ssplit, pos, lemma, parse");;
	    pipeline = new StanfordCoreNLP(props);
	    this.freq = freq;

	}

	@Override
	public Map<String, Double> extractFeatures(String doc) {
		
		try {
		
			Map<String,Double> ret = new HashMap<String, Double>();
			
			List<String> fragments = conn.getDocumentText(doc, fields);
			
			for (int i = 0; i < fragments.size(); i++) {
				
				Map<String,Double> aux = getPOS(fragments.get(i));
				
				for (Entry<String, Double> entry : aux.entrySet()) {
					
					if (freq){
					
						Double val = ret.get(entry.getKey());
						
						if (val == null){
							val = 0.0;
							
						}
						
						ret.put(entry.getKey(), val + entry.getValue());
					}else{
						if (!ret.containsKey(entry.getKey()))
							ret.put(entry.getKey(), 1.0);
					}
				}
				
			}
			
			return ret;
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Map<String, Double> getPOS(String fragment) {
		
		Map<String,Double> ret = new HashMap<String, Double>();
		
	    Annotation document = new Annotation(fragment);
	    
	    pipeline.annotate(document);
	    
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    
	    String comb = null;
	    
	    for(CoreMap sentence: sentences) {

	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {

	    		String word = token.get(TextAnnotation.class);
	    		
	    		String pos = token.get(PartOfSpeechAnnotation.class);
	    		
	    		comb = combine(word,pos);
	    		
	    		if (freq){
	    		
		    		Double val = ret.get(comb);
		    		
		    		if (val == null){
		    			val = 0.0;
		    		}
		    		
		    		ret.put(comb, val + 1.0);
	    		}else{
	    			if (!ret.containsKey(comb))
	    				ret.put(comb, 1.0);
	    		}
	    	}

	    }
		
	    return ret;
	}

	private String combine(String word, String pos) {
		return word + "_" + pos;
	}

	@Override
	public Pair<String, String> getTerm(String term) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Query getQuery(String term) {
		// TODO Auto-generated method stub
		return null;
	}

}
