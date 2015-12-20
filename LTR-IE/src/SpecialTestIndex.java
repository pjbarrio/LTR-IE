import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import edu.columbia.cs.ltrie.indexing.IndexConnector;
import edu.columbia.cs.ltrie.indexing.SimpleBooleanSimilarity;
import edu.columbia.cs.ltrie.indexing.SimpleFrequencySimilarity;


public class SpecialTestIndex {
	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationRealIndex"));
		Directory directory = new SimpleFSDirectory(new File("/proj/dbNoBackup/pjbarrio/Dataset/indexes/NYTValidationNewIndex"));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		Set<String> documents = conn.getAllFiles();
		
		Query query1 = new TermQuery(new Term("BODY", "about"));
		Query query2 = new TermQuery(new Term("BODY", "storm"));
		Map<Query,Double> weights = new HashMap<Query, Double>();
		weights.put(query1, 0.1);
		weights.put(query2, 2.0);
		
		Set<Entry<String, Float>> aux = conn.getScores(weights, new SimpleBooleanSimilarity(),documents).entrySet();
		
		for(Entry<String,Float> entry : aux){
			System.out.println(entry.getKey() + " " + entry.getValue());
		}
		
		System.err.println(aux.size());
	}
}
