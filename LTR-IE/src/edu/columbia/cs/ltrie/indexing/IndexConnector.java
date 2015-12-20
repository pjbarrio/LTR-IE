package edu.columbia.cs.ltrie.indexing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import pt.utl.ist.online.learning.utils.MemoryEfficientHashMap;

import edu.columbia.cs.ltrie.datamodel.DocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.utils.Pair;

public class IndexConnector {

	private static final String DOCUMENT_FIELD = "ORIGINAL_DOCUMENT";
	
	private Analyzer analyzer;
	private Directory directory;
	private IndexWriterConfig config;
	private IndexWriter iwriter = null;
	private DirectoryReader ireader = null;
	private IndexSearcher isearcher = null;
	private Set<String> onlyPath;
	private String path;
	private final String DOCUMENT_INDICATOR = "test";
	private Integer numDocs=null;
	private Map<Integer,String> paths =  new MemoryEfficientHashMap<Integer, String>();
	private Map<String,Integer> pathsInverted =  new MemoryEfficientHashMap<String,Integer>();

	public IndexConnector(Analyzer analyzer, Directory directory, String path){
		this.analyzer=analyzer;
		this.directory=directory;
		this.config = new IndexWriterConfig(Version.LUCENE_CURRENT, analyzer);
		this.path = path;
	}

	public void addDocument(DocumentWithFields docFields) throws IOException{
		if(iwriter==null){
			iwriter = new IndexWriter(directory, config);
		}

		Document luceneDoc = new Document();
		Set<String> fields = docFields.getFields();
		luceneDoc.add(new VecTextField(DOCUMENT_INDICATOR, DOCUMENT_INDICATOR, Store.NO));
		for(String field : fields){
			if(!field.equals(DocumentWithFields.PATH_FIELD) && !field.equals(DOCUMENT_FIELD)){
				StringBuffer buf = new StringBuffer();
				List<String> fieldValues = docFields.getField(field);
				boolean isFirst=true;
				for(String value : fieldValues){
					if(isFirst){
						buf.append(value);
						isFirst=false;
					}else{
						buf.append("\n\n" + value);
					}
				}

				luceneDoc.add(new VecTextField(field, buf.toString(), Store.NO));
			}else{
				StringBuffer buf = new StringBuffer();
				List<String> fieldValues = docFields.getField(field);
				boolean isFirst=true;
				for(String value : fieldValues){
					if(isFirst){
						buf.append(value);
						isFirst=false;
					}else{
						buf.append("\n\n" + value);
					}
				}

				luceneDoc.add(new VecTextField(field, buf.toString(), Store.YES));
			}
		}
		iwriter.addDocument(luceneDoc);
	}


	public Map<String, Integer> getTermFrequencies(String path, String field) throws IOException, ParseException{
		Map<String, Integer> frequencies = new HashMap<String, Integer>();
		Integer docId = getDocId(path);
		if(docId==null){
			return frequencies;
		}
		Terms terms = ireader.getTermVector(docId,field);
		if(terms!=null){
			TermsEnum termsEnum = null;
			termsEnum = terms.iterator(termsEnum);
			BytesRef byteRef = null;
			while ((byteRef = termsEnum.next()) != null) {
				String term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
				int freq = (int) termsEnum.totalTermFreq();
				frequencies.put(term, freq);
			}
		}

		return frequencies;
	}

	public Set<Term> getDocumentTerms(String path) throws IOException, ParseException{
		Set<Term> result = new HashSet<Term>();
		Integer docId = getDocId(path);
		if(docId==null){
			return result;
		}

		Fields fields = ireader.getTermVectors(docId);
		Iterator<String> iter = fields.iterator();
		while(iter.hasNext()){
			String field = iter.next();
			Terms terms = fields.terms(field);
			if(terms!=null){
				TermsEnum termsEnum = null;
				termsEnum = terms.iterator(termsEnum);
				BytesRef byteRef = null;
				while ((byteRef = termsEnum.next()) != null) {
					String term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
					int freq = (int) termsEnum.totalTermFreq();
					Term t = new Term(field,byteRef);
					result.add(t);
				}
			}
		}

		return result;
	}

	public Map<String,Double> getDocumentFrequencies(String field) throws IOException{
		if(ireader==null){
			ireader=DirectoryReader.open(directory);
		}

		double numDocs = getNumDocuments();

		HashMap<String,Double> result = new HashMap<String,Double>();

		Fields fields = MultiFields.getFields(ireader);
		Terms terms = fields.terms(field);
		TermsEnum iterator = terms.iterator(null);
		BytesRef byteRef = null;
		while((byteRef = iterator.next()) != null) {
			String term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
			double docFreq = iterator.docFreq();
			result.put(term,docFreq/numDocs);
		}

		return result;
	}


	public Integer getDocId(String path) throws IOException, ParseException{
		Integer docId = pathsInverted.get(path);
		if(docId==null){
			if(ireader==null){
				ireader=DirectoryReader.open(directory);
				isearcher = new IndexSearcher(ireader);
			}
	
			if (path!=null)
				path = path.replace(this.path, "");
	
			MultiFieldQueryParser luceneParser = new MultiFieldQueryParser(Version.LUCENE_CURRENT, 
					new String[]{NYTDocumentWithFields.PATH_FIELD}, new KeywordAnalyzer());
			Query query = luceneParser.parse(path);
			ScoreDoc[] hits = isearcher.search(query, null, 1).scoreDocs;
			if(hits.length==1){
				paths.put(hits[0].doc,path);
				pathsInverted.put(path, hits[0].doc);
				return hits[0].doc;
			}else{
				return null;
			}
		}
		return docId;
	}

	public void closeWriter() throws IOException{
		iwriter.close();
		iwriter=null;
	}

	public void closeReader() throws IOException{
		ireader.close();
		ireader=null;
	}

	public List<Integer> searchWithoutOrder(Query query) throws IOException{
		if(ireader==null){
			ireader=DirectoryReader.open(directory);
			isearcher = new IndexSearcher(ireader);
		}

		NoSortCollector collector = new NoSortCollector();
		isearcher.search(query, collector);

		List<Integer> ret = new ArrayList<Integer>(collector.hits);	

		//System.out.println(ret.size() + " " + search(query,Integer.MAX_VALUE).size());

		return ret;
	}
	
	public List<Integer> search(Query query) throws IOException{
		return search(query,Integer.MAX_VALUE);
	}

	public List<Integer> search(Query query, int hitsPerQuery) throws IOException {
		if(ireader==null){
			ireader=DirectoryReader.open(directory);
			isearcher = new IndexSearcher(ireader);
		}

		TopDocs hits = isearcher.search(query, null, hitsPerQuery);

		List<Integer> ret = new ArrayList<Integer>(hits.totalHits);		

		for (int i = 0; i < hits.totalHits && i < hitsPerQuery; i++) {
			ret.add(hits.scoreDocs[i].doc);
		}

		return ret;

	}

	public List<Integer> search(Query query, int hitsPerQuery, Similarity sim) throws IOException {
		if(ireader==null){
			ireader=DirectoryReader.open(directory);
		}

		IndexSearcher isearcher = new IndexSearcher(ireader);
		isearcher.setSimilarity(sim);

		TopDocs hits = isearcher.search(query, null, hitsPerQuery);

		List<Integer> ret = new ArrayList<Integer>(hits.totalHits);		

		for (int i = 0; i < hits.totalHits && i < hitsPerQuery; i++) {
			ret.add(hits.scoreDocs[i].doc);
		}

		return ret;
	}

	public Map<String, Float> getScores(Map<Query, Double> weights,
			SimpleBooleanSimilarity sim,
			Set<String> collection, Set<String> containerDocuments) throws IOException {
		return getScores(weights, sim, collection, true, containerDocuments);
	}

	
	public Map<String,Float> getScores(Map<Query,Double> weights, Similarity sim, Set<String> collection) throws IOException{
		
		return getScores(weights, sim, collection, true, new HashSet<String>());
		
	}

	public Map<String,Float> getScores(Map<Query,Double> weights, Similarity sim, Set<String> collection, boolean fillZeros, Set<String> knownDocuments) throws IOException{
		if(ireader==null){
			ireader=DirectoryReader.open(directory);
		}

		Map<String,Float> result = new HashMap<String, Float>();
		
		if (!weights.isEmpty()){
			
			BooleanQuery.setMaxClauseCount(Math.max(weights.size(), knownDocuments.size()));

			BooleanQuery containQuery = new BooleanQuery();
			
			Filter docsFilter = null;
			
			if (!knownDocuments.isEmpty()){
				
				System.out.println("Known Docs: " + knownDocuments.size());
				
				for (String doc : knownDocuments) {

					Query query = new TermQuery(new Term(DOCUMENT_FIELD, doc));
					
					containQuery.add(query, Occur.SHOULD);
				}

				docsFilter = new QueryWrapperFilter(containQuery);
				
			}		
			
			
			
			IndexSearcher isearcher = new IndexSearcher(ireader);
			isearcher.setSimilarity(sim);

			BooleanQuery booleanQuery = new BooleanQuery();
			for(Entry<Query,Double> entry : weights.entrySet()){
				Query query = entry.getKey();
				Set<Term> terms = new HashSet<Term>();
				query.extractTerms(terms);
				query.setBoost(entry.getValue().floatValue()/(terms.size()*terms.size()));
				booleanQuery.add(query, Occur.SHOULD);
			}
			
			TopDocs hits = null;
			
			if (!knownDocuments.isEmpty()){
				hits = isearcher.search(booleanQuery, docsFilter, Integer.MAX_VALUE);
			} else {
				hits = isearcher.search(booleanQuery, null, Integer.MAX_VALUE);
			}
			
			System.out.println("Hits: " + hits.totalHits);
			for (int i = 0; i < hits.totalHits; i++) {
				int document = hits.scoreDocs[i].doc;
				float score = hits.scoreDocs[i].score;
				String docPath = getPath(document);
				if(collection.contains(docPath)){
					result.put(docPath, score);
				}else{
					//System.out.print(".");
				}
			}
			
		}
		
		

		if (fillZeros){
		
			for(String doc : collection){
				if(!result.containsKey(doc)){
					result.put(doc, 0.0f);
				}
			}

		}
		
		return result;
	}
	
	
	public List<Pair<String,Float>> getScoresOrdered(Map<Query,Double> weights, Similarity sim, Set<String> collection, Set<String> knownDocuments) throws IOException{
		if(ireader==null){
			ireader=DirectoryReader.open(directory);
		}

		List<Pair<String,Float>> result = new ArrayList<Pair<String,Float>>();
		
		if (!weights.isEmpty()){
			
			BooleanQuery.setMaxClauseCount(Math.max(weights.size(), knownDocuments.size()));

			BooleanQuery containQuery = new BooleanQuery();
			
			Filter docsFilter = null;
			
			if (!knownDocuments.isEmpty()){
				
				System.out.println("Known Docs: " + knownDocuments.size());
				
				for (String doc : knownDocuments) {

					Query query = new TermQuery(new Term(DOCUMENT_FIELD, doc));
					
					containQuery.add(query, Occur.SHOULD);
				}

				docsFilter = new QueryWrapperFilter(containQuery);
				
			}		
			
			
			
			IndexSearcher isearcher = new IndexSearcher(ireader);
			isearcher.setSimilarity(sim);

			BooleanQuery booleanQuery = new BooleanQuery();
			for(Entry<Query,Double> entry : weights.entrySet()){
				Query query = entry.getKey();
				Set<Term> terms = new HashSet<Term>();
				query.extractTerms(terms);
				query.setBoost(entry.getValue().floatValue()/(terms.size()*terms.size()));
				booleanQuery.add(query, Occur.SHOULD);
			}
			
			TopDocs hits = null;
			
			if (!knownDocuments.isEmpty()){
				hits = isearcher.search(booleanQuery, docsFilter, Integer.MAX_VALUE);
			} else {
				hits = isearcher.search(booleanQuery, null, Integer.MAX_VALUE);
			}
			
			System.out.println("Hits: " + hits.totalHits);
			for (int i = 0; i < hits.totalHits; i++) {
				int document = hits.scoreDocs[i].doc;
				float score = hits.scoreDocs[i].score;
				String docPath = getPath(document);
				if(collection.contains(docPath)){
					result.add(new Pair<String,Float>(docPath, score));
				}else{
					//System.out.print(".");
				}
			}
			
		}
		
		return result;
	}
	
	public String getPath(Integer docId) throws IOException {
		String p = paths.get(docId);
		if(p==null){
			if(ireader==null){
				ireader=DirectoryReader.open(directory);
				onlyPath = new HashSet<String>(1);
				onlyPath.add(DocumentWithFields.PATH_FIELD);
			}

			Document d = ireader.document(docId, onlyPath);

			p = path + d.getField(DocumentWithFields.PATH_FIELD).stringValue();
			paths.put(docId, p);
			pathsInverted.put(p, docId);
		}
		return p;
	}

	public Set<String> getAllFiles() throws IOException{
		if(ireader==null){
			ireader=DirectoryReader.open(directory);
			isearcher = new IndexSearcher(ireader);
		}

		Query matchAllDocuments = new MatchAllDocsQuery();
		NoSortCollector collector = new NoSortCollector();
		isearcher.search(matchAllDocuments, collector);

		Set<Integer> docs = new HashSet<Integer>(collector.hits);	

		Set<String> result = new HashSet<String>();

		for (Integer doc : docs) {
			result.add(getPath(doc));
		}

		return result;
	}
	
	public int getNumDocuments() throws IOException{
		
		if(numDocs==null){
			if(ireader==null)
				ireader=DirectoryReader.open(directory);

			numDocs=ireader.numDocs();
		}
		return numDocs;
				
	}


	private class NoSortCollector extends Collector {
		private Set<Integer> hits = new HashSet<Integer>();
		private int docBase;

		@Override
		public void setScorer(Scorer scorer) {
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return true;
		}

		@Override
		public void collect(int doc) {
			hits.add(docBase+doc);
		}

		@Override
		public void setNextReader(AtomicReaderContext context) {
			this.docBase = context.docBase;
		}
	}

	public List<String> getDocumentText(String doc, Set<String> fields) throws IOException, ParseException {
		
		Document document = ireader.document(getDocId(doc));
		
		List<String> ret = new ArrayList<String>();
		
		for (String field : fields) {
			
			ret.addAll(Arrays.asList(document.getValues(field)));
			
		}
		
		return ret;
		
	}

}
