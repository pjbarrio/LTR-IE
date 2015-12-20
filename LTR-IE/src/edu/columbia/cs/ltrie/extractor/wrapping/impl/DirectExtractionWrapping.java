package edu.columbia.cs.ltrie.extractor.wrapping.impl;

import java.io.IOException;
import java.util.List;

import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.extractor.ExtractionSystem;
import edu.columbia.cs.ltrie.extractor.wrapping.ExtractionWrapper;

public class DirectExtractionWrapping implements
		ExtractionWrapper {

	private ExtractionSystem ieSystem;
	
	public DirectExtractionWrapping(ExtractionSystem ieSystem){
		this.ieSystem = ieSystem;
	}
	
	@Override
	public int getNumTuplesDocument(String doc) throws IOException {
		return ieSystem.extractTuplesFrom(doc).size();
	}

	@Override
	public List<Tuple> getTuplesDocument(String doc) throws IOException {
		return ieSystem.extractTuplesFrom(doc);
	}

}
