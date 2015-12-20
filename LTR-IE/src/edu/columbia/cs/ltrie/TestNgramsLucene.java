package edu.columbia.cs.ltrie;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import com.nytlabs.corpus.NYTCorpusDocument;
import com.nytlabs.corpus.NYTCorpusDocumentParser;

import edu.columbia.cs.ltrie.datamodel.DocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class TestNgramsLucene {
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
		ShingleAnalyzerWrapper shingleAnalyzer = new ShingleAnalyzerWrapper(analyzer, 2, 2, " ", false, false);
		Directory directory = new RAMDirectory();
		IndexConnector conn = new IndexConnector(shingleAnalyzer, directory, "");
		for(DocumentWithFields docFields : documentsWithFields){
			conn.addDocument(docFields);
		}
		conn.closeWriter();
		
		for(DocumentWithFields docFields : documentsWithFields){
			conn.getTermFrequencies(docFields.getPath(), NYTDocumentWithFields.BODY_FIELD);
		}
	}
}
