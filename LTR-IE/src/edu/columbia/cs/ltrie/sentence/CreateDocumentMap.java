package edu.columbia.cs.ltrie.sentence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import edu.columbia.cs.ltrie.indexing.IndexConnector;
import edu.columbia.cs.ltrie.utils.SerializationHelper;

public class CreateDocumentMap {

	private static final String DOC_SENT_MAP = ".doc_sent.map";
	private static final String DOCUMENT_FIELD = "ORIGINAL_DOCUMENT";

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		String task = args[0]; //e.g., "LTrain";
		String outprefix = args[1]; //e.g., "data/omp/test1/";
		
		String directo_docs = "/proj/db-files2/NoBackup/pjbarrio/Dataset/indexes/NYTTrain100Index";
		String directo = "/proj/db-files2/NoBackup/pjbarrio/Dataset/indexes/NYTTrain100Index-Sentence";
		
		System.out.println("Create Document--Sentence map");

		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		
		Directory directory_docs = new SimpleFSDirectory(new File(directo_docs));
		IndexConnector conn_docs = new IndexConnector(analyzer, directory_docs, "");
		
		Set<String> docs = conn_docs.getAllFiles();
		
		conn_docs.closeReader();
		
		Directory directory = new SimpleFSDirectory(new File(directo));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");

		Map<String,List<Long>> docMap = new HashMap<String, List<Long>>();
		
		int updated = 0;
		
		for(String doc : docs){
		
			if (updated % 1000 == 0){
				System.out.println("Udated: " + updated + " of " + docs.size());
			}
			
			BooleanQuery containQuery = new BooleanQuery();
			BooleanQuery.setMaxClauseCount(1);
			
			containQuery.add( new TermQuery(new Term(DOCUMENT_FIELD, doc)), Occur.MUST);
			
			List<Integer> local_sents = conn.search(containQuery);
			
			System.out.println(doc + " - " + local_sents.size());

			List<Long> idss = new ArrayList<Long>(local_sents.size()); 
			
			for (int i = 0; i < local_sents.size(); i++) {
				
				idss.add((long)local_sents.get(i));
				
			}
			
			docMap.put(doc, idss);
			
			updated++;
			
		}
		
		SerializationHelper.write(getDocSentMapName(outprefix,task), docMap);

	}

	public static String getDocSentMapName(String outprefix, String task) {
		return outprefix + task + DOC_SENT_MAP;
	}
	
}
