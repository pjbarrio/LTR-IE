package edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

public abstract class InitialWordLoader {

	private QueryParser qp;

	public InitialWordLoader(QueryParser qp) {
		this.qp = qp;
	}

	public List<Query> getInitialQueries() throws ParseException, IOException{
		
		List<String> strQueries = getInitialStringQueries();
		
		List<Query> ret = new ArrayList<Query>(strQueries.size());
		
		for (int i = 0; i < strQueries.size(); i++) {
			
			ret.add(qp.parse(strQueries.get(i)));
			
		}
		
		return ret;
		
	}
	
	protected abstract List<String> getInitialStringQueries() throws IOException;
	
}
