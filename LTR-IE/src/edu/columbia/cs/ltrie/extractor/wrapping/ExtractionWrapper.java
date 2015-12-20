package edu.columbia.cs.ltrie.extractor.wrapping;

import java.io.IOException;
import java.util.List;

import edu.columbia.cs.ltrie.datamodel.Tuple;

public interface ExtractionWrapper {
	public int getNumTuplesDocument(String doc) throws IOException;
	public List<Tuple> getTuplesDocument(String doc) throws IOException;
}
