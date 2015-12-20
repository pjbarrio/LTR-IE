package edu.columbia.cs.ltrie.features;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;

import pt.utl.ist.online.learning.utils.Pair;

import edu.columbia.cs.ltrie.datamodel.DocumentWithFields;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class TermFrequencyFeatureExtractor extends FeatureExtractor {
	private static final String LABEL = "TF";
	private IndexConnector conn;
	private String field;
	private String label;
	private boolean computeAbsolute;
	private boolean computeRelative;
	private boolean computeBoolean;
	private final Double ONE = new Double(1.0);
	private Map<Integer,Double> frequencies = new HashMap<Integer,Double>();

	public TermFrequencyFeatureExtractor(IndexConnector conn, String field){
		this(conn,field,true,true,true);
	}

	public TermFrequencyFeatureExtractor(IndexConnector conn, String field,
			boolean computeAbsolute, boolean computeRelative,
			boolean computeBoolean){
		this.conn=conn;
		this.field=field;
		this.label=LABEL + "_" + field;
		this.computeAbsolute=computeAbsolute;
		this.computeRelative=computeRelative;
		this.computeBoolean=computeBoolean;
	}
	

	public Map<String,Double> extractFeatures(String doc){
		Map<String,Double> d = new HashMap<String, Double>();
		try {
			
			Map<String, Integer> frequencies = conn.getTermFrequencies(doc, field);
			double sum = 0.0;
			if(computeRelative){
				for(Integer val : frequencies.values()){
					sum+=val;
				}
			}
			for(Entry<String,Integer> entry : frequencies.entrySet()){
				int value = entry.getValue();
				if(computeAbsolute){
					Double v = this.frequencies.get(value);
					if(v==null){
						v=(double) value;
						this.frequencies.put(value, v);
					}

					d.put(label + "_ABS_" + entry.getKey(), v);
				}
				if(computeRelative){
					d.put(label + "_REL_" + entry.getKey(), value/sum);
				}
				if(computeBoolean){
					d.put(label + "_BOO_" + entry.getKey(), ONE);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(1);
		}

		return d;
	}

	@Override
	public Pair<String,String> getTerm(String term) {
		String[] splitedKey = term.split("_");
		String field=null;
		String value=null;
		try{
			field = splitedKey[1];
			value = splitedKey[3];
		}catch(ArrayIndexOutOfBoundsException aiobe){
			System.out.println("Problem:");
			System.out.println("Term: " + term);
			System.out.println("Splited key: " + Arrays.toString(splitedKey));
			System.exit(1);
		}
		//if(splitedKey[2].equals("BOO")){
		return new Pair<String,String>(field,value);
		//}
		//return null;
	}

	@Override
	public Query getQuery(String term) {
		String[] splitedKey = term.split("_");
		String field=null;
		String value=null;
		try{
			field = splitedKey[1];
			value = splitedKey[3];
		}catch(ArrayIndexOutOfBoundsException aiobe){
			System.out.println("Problem:");
			System.out.println("Term: " + term);
			System.out.println("Splited key: " + Arrays.toString(splitedKey));
			System.exit(1);
		}
		Query q = new TermQuery(new Term(field,value));
		return q;
	}
}
