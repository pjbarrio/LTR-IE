import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.indexing.IndexConnector;
import edu.columbia.cs.ltrie.indexing.SimpleBooleanSimilarity;


public class TestQueries {
	public static void main(String[] args) throws IOException, ParseException {
		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationRealFlatIndex"));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		Set<String> collectionFixed = conn.getAllFiles();
		
		/*String[] fieldsVector1 = new String[]{NYTDocumentWithFields.TITLE_FIELD,
				NYTDocumentWithFields.LEAD_FIELD,NYTDocumentWithFields.BODY_FIELD};
		QueryParser qp1 = new MultiFieldQueryParser(
                Version.LUCENE_41, 
                fieldsVector1,
                new StandardAnalyzer(Version.LUCENE_41));
		Map<Query,Double> scores = new HashMap<Query, Double>();
		scores.put(qp1.parse("+death"),1.0);
		
		Map<String,Float> results = conn.getScores(scores, new SimpleBooleanSimilarity(), collectionFixed);
		for(Entry<String,Float> entry : results.entrySet()){
			if(entry.getValue()!=0){
				System.out.println(entry);
			}
		}*/
		
		String[] fieldsVector2 = new String[]{NYTDocumentWithFields.ALL_FIELD};
		QueryParser qp2 = new MultiFieldQueryParser(
                Version.LUCENE_41, 
                fieldsVector2,
                new StandardAnalyzer(Version.LUCENE_41));
		Map<Query,Double> scores2 = new HashMap<Query, Double>();
		scores2.put(qp2.parse("+\"u.s. president\""),1.0);
		
		Map<String,Float> results2 = conn.getScores(scores2, new SimpleBooleanSimilarity(), collectionFixed);
		for(Entry<String,Float> entry : results2.entrySet()){
			if(entry.getValue()!=0){
				System.out.println(entry);
			}
		}
		
	}
}
