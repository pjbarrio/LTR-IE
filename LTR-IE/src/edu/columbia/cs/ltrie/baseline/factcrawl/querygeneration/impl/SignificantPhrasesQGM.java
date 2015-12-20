package edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;

import com.aliasi.lm.TokenizedLM;
import com.aliasi.tokenizer.EnglishStopTokenizerFactory;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.RegExFilteredTokenizerFactory;
import com.aliasi.tokenizer.StopTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.ScoredObject;

import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.QueryGenerationMethod;
import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.indexing.IndexConnector;
import edu.columbia.cs.ltrie.utils.DocumentLoader;

public class SignificantPhrasesQGM extends QueryGenerationMethod{

	private int ngramSize;
	private int minSupport;
	private HashMap<String, String> pathDictionary;

	public SignificantPhrasesQGM(HashMap<String, String> pathDictionary, QueryParser qp, int ngramSize, int minSupport) {
		super(qp);
		this.pathDictionary=pathDictionary;
		this.ngramSize = ngramSize;
		this.minSupport = minSupport;
	}

	@Override
	public List<String> generateStringQueries(int numberOfQueries,List<String> relevant, List<String> nonRelevant, Map<String,List<Tuple>> tuples) throws IOException {
				
		List<String> content = new ArrayList<String>();
		
		
		for (int i = 0; i < relevant.size(); i++) {
			content.add(DocumentLoader.loadDocument(pathDictionary.get(relevant.get(i))));
		}
		
		Set<String> sW = new HashSet<String>();
		
		for (Iterator<Object> i = StopAnalyzer.ENGLISH_STOP_WORDS_SET.iterator(); i.hasNext();) {
			
			sW.add(i.next().toString());
			
		}
		
//		new HashSet<String>(Arrays.asList(StopAnalyzer.ENGLISH_STOP_WORDS_SET.toArray(new String[0])))
		
		TokenizerFactory tokenizerFactory = new StopTokenizerFactory(new EnglishStopTokenizerFactory(new LowerCaseTokenizerFactory(new  RegExFilteredTokenizerFactory(new IndoEuropeanTokenizerFactory(), Pattern.compile("[^\\p{Punct}]*")))),sW);		
		TokenizedLM backgroundModel = buildModel(tokenizerFactory,ngramSize,content);

		backgroundModel.sequenceCounter().prune(minSupport);
		SortedSet<ScoredObject<String[]>> coll = backgroundModel.collocationSet(ngramSize,minSupport,numberOfQueries);		
		List<String> list = report(coll,numberOfQueries);
				
		return list;
	
	}

	private TokenizedLM buildModel(TokenizerFactory tokenizerFactory,
			int ngram, List<String> contents)
					throws IOException {

		TokenizedLM model = new TokenizedLM(tokenizerFactory,ngram);

		for (int j = 0; j < contents.size(); ++j) {
			
			model.handle(contents.get(j));
		
		}
		return model;
	}
	
	private List<String> report(SortedSet<ScoredObject<String[]>> nGrams, int limit) {
	    
		List<String> ret = new ArrayList<String>(Math.min(nGrams.size(), limit));
		
		for (ScoredObject<String[]> nGram : nGrams) {
	        String[] toks = nGram.getObject();
	        ret.add(generateString(toks));
	    }
		
		return ret;
	}
	    
	private String generateString(String[] toks) {
		
		String s = "+\"" + toks[0];
		
		for (int i = 1; i < toks.length; i++) {
			s += " " + toks[i];
		}
		
		return s + "\"";
	}

}
