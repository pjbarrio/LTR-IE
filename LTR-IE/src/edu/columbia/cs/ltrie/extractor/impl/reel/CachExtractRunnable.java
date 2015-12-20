package edu.columbia.cs.ltrie.extractor.impl.reel;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.columbia.cs.ref.model.Document;
import etxt2db.api.ClassificationExecutor.ClassifiedSpan;

public class CachExtractRunnable implements Runnable {

	private Document doc;
	private EntityExtractor entityExtractor;
	private Map<String, List<ClassifiedSpan>> res;

	public CachExtractRunnable(Document doc,EntityExtractor entityExtractor, Map<String,List<ClassifiedSpan>> result) {
		this.doc = doc;
		this.entityExtractor = entityExtractor;
		this.res = result;
	}

	@Override
	public void run() {
						
		Map<String, List<ClassifiedSpan>> result;
		
		result = entityExtractor.getClassifiedSpans(doc);
			 
		for (Entry<String,List<ClassifiedSpan>> entry : result.entrySet()) {
			
			res.put(entry.getKey(),entry.getValue());
			
		}

	}

}
