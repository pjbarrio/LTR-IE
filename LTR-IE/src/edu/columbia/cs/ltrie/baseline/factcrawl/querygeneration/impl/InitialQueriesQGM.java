package edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.queryparser.classic.QueryParser;

import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.QueryGenerationMethod;
import edu.columbia.cs.ltrie.datamodel.Tuple;

public class InitialQueriesQGM extends QueryGenerationMethod {
	private List<String> lines;
	
	public InitialQueriesQGM(QueryParser qp, String queriesPath) throws IOException {
		super(qp);
		this.lines = FileUtils.readLines(new File(queriesPath));
	}

	@Override
	public List<String> generateStringQueries(int numberOfQueries,List<String> relevant, List<String> nonRelevant, Map<String,List<Tuple>> tuples) {
		
		List<String> ret = new ArrayList<String>(lines.size());
		
		for (int i = 0; i < lines.size(); i++) {
			ret.add("+" + lines.get(i).substring(lines.get(i).indexOf(",")+1));
		}
		
		return ret;
	
	}

}
