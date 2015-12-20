package edu.columbia.cs.ltrie.baseline.factcrawl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

import edu.columbia.cs.ltrie.baseline.factcrawl.utils.MapBasedComparator;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;

public class QueryParserTest {

	/**
	 * @param args
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws ParseException {

		Map<Integer,Double> docScores = new HashMap<Integer, Double>();
		docScores.put(1, 0.5);
		docScores.put(2, 1.0);
		
		List<Integer> d = new ArrayList<Integer>(docScores.keySet());
		
		Collections.sort(d,new MapBasedComparator<Integer>(docScores,true));
		
		System.out.println(d.toString());
		
		QueryParser qp = new QueryParser(Version.LUCENE_CURRENT, NYTDocumentWithFields.BODY_FIELD,  new StandardAnalyzer(Version.LUCENE_CURRENT));
		
		Query q = qp.parse("\"hello world\" AND \"house\" ");
		
		Query q2 = qp.parse("\"hello world\" +\"house\" ");
		
		System.out.println(q + " - " + q2);
		
		Set<Query> qs = new HashSet<Query>();
		
		System.out.println(q.hashCode());
		System.out.println(q2.hashCode());
		
		System.out.println(qs.add(q));
		
		System.out.println(qs.add(q2));
		
	}

}
