package edu.columbia.cs.ltrie;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import com.nytlabs.corpus.NYTCorpusDocument;
import com.nytlabs.corpus.NYTCorpusDocumentParser;

import edu.columbia.cs.ltrie.datamodel.DocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.UnsupportedFieldNameException;
import edu.columbia.cs.ltrie.indexing.IndexConnector;
import edu.columbia.cs.ltrie.utils.DocumentLoader;
import edu.columbia.cs.ltrie.utils.NYTDocumentLoader;
import edu.columbia.cs.ref.model.Document;
import edu.columbia.cs.ref.model.Segment;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class IndexCollectionAsSentences {

	private static final String DOCUMENT_FIELD = "ORIGINAL_DOCUMENT";
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws UnsupportedFieldNameException 
	 */
	public static void main(String[] args) throws IOException, UnsupportedFieldNameException {
				
		String path = args[0];
		File pathF = new File(path);
		int num = Integer.parseInt(args[2]);
		int numPathsTotal=pathF.list().length;
		int numPaths=num* numPathsTotal/100;
		String[] subPaths = new String[numPaths];
		String folderDesign = "%03d";//"%0" + String.valueOf(numPathsTotal).length() + "d";
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format(folderDesign, i);
		}
		
		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		//analyzer = new ShingleAnalyzerWrapper(analyzer, 2, 2);
		Directory directory = new SimpleFSDirectory(new File(args[1] + num +"Index-Sentence"));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		indexCollection(path,subPaths,conn);
		
	}
	
	private static void indexCollection(String path, String[] subPaths, IndexConnector conn) throws IOException, UnsupportedFieldNameException {
		
		Properties props = new Properties();
	    props.setProperty("annotators", "tokenize, ssplit");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

	    Set<String> indexedDocs = new HashSet<String>();
		
		for(String p : subPaths){
			indexDocs(path + p, conn, pipeline, indexedDocs);
		}
		
		System.out.println("Added: " + indexedDocs.size());
		
	}
	
	private static void indexDocs(String path, IndexConnector conn, StanfordCoreNLP pipeline, Set<String> indexedDocs) throws IOException, UnsupportedFieldNameException{
		System.out.println("Load Documents in " + path);
		
		NYTDocumentLoader loader = new NYTDocumentLoader();
				
		System.out.println("Load Index");		
		
		File fDir = new File(path);
		for(File f : fDir.listFiles()){

			List<DocumentWithFields> documentsWithFields = new ArrayList<DocumentWithFields>();
			
			String content = DocumentLoader.loadDocument(f.getAbsolutePath());
			
			Document doc = loader.loadFile(f.getAbsolutePath(), content);
			
			int i = 1;

			for (Segment segment : doc.getPlainText()) {
				
				Annotation document = new Annotation(segment.getValue());
			    
			    // run all Annotators on this text
			    pipeline.annotate(document);
			    
			    // these are all the sentences in this document
			    // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
			    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			    
			    for(CoreMap sentence: sentences) {
			    	String texts = sentence.get(TextAnnotation.class);
			    	NYTCorpusDocument aux = new NYTCorpusDocument();
					aux.setBody(texts);
					
					String fnam = f.getAbsolutePath() + "." + i++;
					
					if (!indexedDocs.add(fnam)){
						System.out.println("duplicated:" + fnam);
					}
					aux.setSourceFile(new File(fnam));
					NYTDocumentWithFields docwf = new NYTDocumentWithFields(aux);
					docwf.addField(DOCUMENT_FIELD, f.getName());
					documentsWithFields.add(docwf);
			    }
				
			}
		
			
			
			for(DocumentWithFields docFields : documentsWithFields){
				conn.addDocument(docFields);
			}
		
		}
		
		
		conn.closeWriter();
		
	}

}
