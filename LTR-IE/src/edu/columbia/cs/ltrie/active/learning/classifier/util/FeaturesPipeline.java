package edu.columbia.cs.ltrie.active.learning.classifier.util;

import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class FeaturesPipeline {

	private StanfordCoreNLP pipeline;

	public FeaturesPipeline(String tools){
		
		Properties props = new Properties();
		
		//"tokenize, ssplit, pos, lemma", ner, parse, dcoref"
		props.put("annotators", tools);
	    
		pipeline = new StanfordCoreNLP(props);
		
	}
	
	public Annotation process(String text) {
		
  
	    // create an empty Annotation just with the given text
	    Annotation document = new Annotation(text);
	    
	    // run all Annotators on this text
	    pipeline.annotate(document);
	    
	    return document;
	    
	}
	
}
