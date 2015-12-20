package edu.columbia.cs.ltrie;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import com.nytlabs.corpus.NYTCorpusDocument;
import com.nytlabs.corpus.NYTCorpusDocumentParser;

import edu.columbia.cs.ltrie.datamodel.DocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.features.FeaturesCoordinator;
import edu.columbia.cs.ltrie.features.TermFrequencyFeatureExtractor;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class TestIndexing {
	public static void main(String[] args) throws IOException, ParseException {
		String path = "/home/goncalo/NYTValidationSplit/002/";
		File fDir = new File(path);
		
		System.out.println("Load Document");
		NYTCorpusDocumentParser parser = new NYTCorpusDocumentParser();
		List<DocumentWithFields> documentsWithFields = new ArrayList<DocumentWithFields>();
		for(File f : fDir.listFiles()){
			NYTCorpusDocument doc = parser.parseNYTCorpusDocumentFromFile(f, false);
			documentsWithFields.add(new NYTDocumentWithFields(doc));
		}
		
		System.out.println("Load Index");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		Directory directory = new RAMDirectory();
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		for(DocumentWithFields docFields : documentsWithFields){
			conn.addDocument(docFields);
		}
		conn.closeWriter();
	    
	    /*Map<String, Integer> docFrequencies = new HashMap<String, Integer>();
	    for(int i=0; i<ireader.maxDoc(); i++){
	    	Fields fields = ireader.getTermVectors(i);
	    	Terms terms = fields.terms(NYTDocumentWithFields.BODY_FIELD);
	    	TermsEnum termsEnum = null;
	        termsEnum = terms.iterator(termsEnum);
	        Map<String, Integer> frequencies = new HashMap<String, Integer>();
	        BytesRef text = null;
	        while ((text = termsEnum.next()) != null) {
	            String term = text.utf8ToString();
	            int freq = (int) termsEnum.totalTermFreq();
	            frequencies.put(term, freq);
	            Term t = new Term(NYTDocumentWithFields.BODY_FIELD,text);
	            if(!docFrequencies.containsKey(term)){
	            	docFrequencies.put(term, ireader.docFreq(t));
	            }
	        }
	        
	        //System.out.println(frequencies);
	    }
	    
        System.out.println("DF: " + docFrequencies);

	    /*ireader.getTermVectors(docID);*/
        
		System.out.println("Compute Features");
	    FeaturesCoordinator coordinator = new FeaturesCoordinator();
	    coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.TITLE_FIELD));
	    coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.LEAD_FIELD));
	    coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.BODY_FIELD));
	    
        for(DocumentWithFields d : documentsWithFields){
        	coordinator.getFeatures(d.getPath()).size();
        }
	    
        System.out.println("Num. features: " + coordinator.getCurrentNumberFeatures());
        
	    directory.close();
	}
}
