import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.AdditiveFileSystemWrapping;
import edu.columbia.cs.ltrie.indexing.IndexConnector;
import edu.columbia.cs.ltrie.indexing.SimpleBooleanSimilarity;
import edu.columbia.cs.ltrie.indexing.SimpleFrequencySimilarity;


public class SimpleTestIndex {
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException, ParseException {
		
		String relationship = "NaturalDisaster";
		String path = "/local/pjbarrio/Files/Downloads/NYTValidationSplit/";
		File pathF = new File(path);
		int numPaths=pathF.list().length;
		String[] subPaths = new String[numPaths];
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format("%03d", i);
		}
		String extractor = "Pablo-Sub-sequences";
		
		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_41);
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationRealIndex"));
		Directory directory = new SimpleFSDirectory(new File("/proj/dbNoBackup/pjbarrio/Dataset/indexes/NYTValidationRealIndex"));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		Set<String> documents = conn.getAllFiles();
		
		String resultsPath = "results" + relationship;
		System.out.println("Initiating IE programs");
		AdditiveFileSystemWrapping extractWrapper = new AdditiveFileSystemWrapping();
		for(String subPath : subPaths){
			if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
				extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationship + ".data");
			}else{
				extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + ".data");
			}
		}
		
		String[] fieldsVector = new String[]{NYTDocumentWithFields.BODY_FIELD};
		
		QueryParser qp = new MultiFieldQueryParser(
                Version.LUCENE_41, 
                fieldsVector,
                new StandardAnalyzer(Version.LUCENE_41));
		
		String quer = "+tornado";
		Query query1 = qp.parse(quer);
		
//		Query query1 = new TermQuery(new Term(NYTDocumentWithFields.ALL_FIELD, "Pentagon"));
		
		List<String> retrievedDocuments = new ArrayList<String>();
		
		System.out.println(conn.search(query1).size());
		
		for(Integer docId : conn.search(query1)){
			retrievedDocuments.add(conn.getPath(docId));
		}
		
		int relevantRet=0;
		int nonRelevantRet=0;
		
		int count = 0;
		
		for(String doc : retrievedDocuments){
			if(extractWrapper.getNumTuplesDocument(doc)!=0){
				relevantRet++;
			}else{
				nonRelevantRet++;
			}
			
			count++;
			System.out.println(count + "," + relevantRet);
			
		}
		documents.removeAll(retrievedDocuments);
		int relevantNRet=0;
		int nonRelevantNRet=0;
		for(String doc : documents){
			if(extractWrapper.getNumTuplesDocument(doc)!=0){
//				System.out.println(conn.getDocumentText(doc, new HashSet<String>(Arrays.asList(new String[]{"BODY"}))));
				relevantNRet++;
			}else{
				nonRelevantNRet++;
			}
		}
		System.out.println(relevantRet + " " + nonRelevantRet + " " + ((double)relevantRet*100.0/(double)(relevantRet+nonRelevantRet)));
		System.out.println(relevantNRet + " " + nonRelevantNRet + " " + ((double)relevantNRet*100.0/(double)(relevantNRet+nonRelevantNRet)));
		System.out.println(relevantNRet+relevantRet+nonRelevantNRet+nonRelevantRet);
	}
}
