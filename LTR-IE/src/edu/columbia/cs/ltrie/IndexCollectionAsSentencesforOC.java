package edu.columbia.cs.ltrie;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import edu.columbia.cs.ltrie.extractor.impl.opencalais.RDFPESExtractor;
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

public class IndexCollectionAsSentencesforOC {

	private static final String DOCUMENT_FIELD = "ORIGINAL_DOCUMENT";

	/**
	 * @param args
	 * @throws IOException 
	 * @throws UnsupportedFieldNameException 
	 */
	public static void main(String[] args) throws IOException, UnsupportedFieldNameException {

		String path = args[0];

		String task = args[1];

		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		//analyzer = new ShingleAnalyzerWrapper(analyzer, 2, 2);
		Directory directory = new SimpleFSDirectory(new File(path, task + "-Index-Sentence"));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		indexCollection(task,conn);

	}

	private static void indexCollection(String task, IndexConnector conn) throws IOException, UnsupportedFieldNameException {

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		indexDocs(ExtractOpenCalaisResultsForDatabase.getFilesPath(task), 
				ExtractOpenCalaisResultsForDatabase.getFilesPathFixed(task), conn, pipeline);

	}

	private static void indexDocs(String path, String fixedPath, IndexConnector conn, StanfordCoreNLP pipeline) throws IOException, UnsupportedFieldNameException{
		System.out.println("Load Documents in " + path);

		System.out.println("Load Index");		

		File fDir = new File(path);

		File[] files = fDir.listFiles();

		String[] filesFixed = new File(fixedPath).list();

		Arrays.sort(filesFixed);

		int count = 0;

		for(File f : files){

			List<CoreMap> sentences = new ArrayList<CoreMap>();
			
			List<DocumentWithFields> documentsWithFields = new ArrayList<DocumentWithFields>();

			String content = RDFPESExtractor.extractContent(f.toURI());

			if (content == null){ //Error

				for (int i = 0; i < filesFixed.length; i++) {

					if (filesFixed[i].contains(f.getName())){

						String scont = RDFPESExtractor.extractContent(new File(fixedPath,filesFixed[i]).toURI());

						if (scont != null){
						
							Annotation document = new Annotation(scont);
	
							// run all Annotators on this text
							pipeline.annotate(document);
	
							// these are all the sentences in this document
							// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
							sentences.addAll(document.get(SentencesAnnotation.class));

						}
							
					}

				}

			} else {

				Annotation document = new Annotation(content);

				// run all Annotators on this text
				pipeline.annotate(document);

				// these are all the sentences in this document
				// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
				sentences.addAll(document.get(SentencesAnnotation.class));

				

			}

			int i = 1;
			
			for(CoreMap sentence: sentences) {
				String texts = sentence.get(TextAnnotation.class);
				NYTCorpusDocument aux = new NYTCorpusDocument();
				aux.setBody(texts);

				String fnam = f.getAbsolutePath() + "." + i++;

				aux.setSourceFile(new File(fnam));
				NYTDocumentWithFields docwf = new NYTDocumentWithFields(aux);
				docwf.addField(DOCUMENT_FIELD, f.getName());
				documentsWithFields.add(docwf);
			}
			
			for(DocumentWithFields docFields : documentsWithFields){
				conn.addDocument(docFields);
			}


			if (count%1000 == 0)
				System.out.println(count + " documents processed!");


			count++;

		}

		conn.closeWriter();

	}

}
