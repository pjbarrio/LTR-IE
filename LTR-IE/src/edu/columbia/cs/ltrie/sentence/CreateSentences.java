package edu.columbia.cs.ltrie.sentence;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

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

public class CreateSentences {

	static int error = 0;
	
	public static void main(String[] args) throws IOException, UnsupportedFieldNameException, ParseException {

		String task = args[0]; //e.g., "LTrain";
		String outprefix = args[1]; //e.g., "data/omp/test1/";
		String path = args[2];
		
		int collectionId = 3;
		String directo = "/proj/db-files2/NoBackup/pjbarrio/Dataset/indexes/NYTTrain100Index-Sentence";

		new File(outprefix).mkdirs();

		System.out.println("Saving sentences");

//		databaseWriter dW = new databaseWriter();

		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		Directory directory = new SimpleFSDirectory(new File(directo));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");

		conn.getNumDocuments();
		
		File pathF = new File(path);
		int numPaths=pathF.list().length;
		String[] subPaths = new String[numPaths];
		String folderDesign = "%03d";//"%0" + String.valueOf(numPathsTotal).length() + "d";
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format(folderDesign, i);
		}

		createSentences(collectionId,path,subPaths,conn);

		System.out.println(error);
		
	}

	private static void createSentences(int collectionId, String path, String[] subPaths, IndexConnector conn) throws IOException, UnsupportedFieldNameException, ParseException {

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		
		int numdocs = conn.getNumDocuments();
		
		Map<String,Integer> mapNameId = new HashMap<String,Integer>();
		
		for (int i = 0; i < numdocs; i++) {
			
			mapNameId.put(conn.getPath(i), i);
			
		}
				
		databaseWriter dW = new databaseWriter();
		
		for(String p : subPaths){
			indexDocs(mapNameId,collectionId, path + p, conn, pipeline, dW);
		}

		dW.closeConnection();
		
	}

	private static void indexDocs(Map<String, Integer> mapNameId, int collectionId, String path, IndexConnector conn, StanfordCoreNLP pipeline, databaseWriter dW) throws IOException, UnsupportedFieldNameException, ParseException{
		System.out.println("Load Documents in " + path);

		NYTDocumentLoader loader = new NYTDocumentLoader();

		System.out.println("Load Index");		

		File fDir = new File(path);
				
		for(File f : fDir.listFiles()){

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

					String fnam = f.getName() + "." + i++;

					String docName = f.getName().replace(".xml", "");

					Integer currentSentence = mapNameId.get(fnam);
					
					if (currentSentence == null){
						System.out.println(fnam);
						error++;
					}
						
					
//					System.out.println(currentSentence + ", " + fnam + "," + docName + ", " + texts);
					
					dW.insertSentence(collectionId, docName, currentSentence, texts);

				}

			}

		}


	}

	

}
