package edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.queryparser.classic.QueryParser;

import edu.columbia.cs.ltrie.baseline.factcrawl.querygeneration.QueryGenerationMethod;
import edu.columbia.cs.ltrie.datamodel.Tuple;

public class AttributeValuesQGM extends QueryGenerationMethod {

	public AttributeValuesQGM(QueryParser qp) {
		super(qp);
	}

	@Override
	public List<String> generateStringQueries(int numberOfQueries,List<String> relevant, List<String> nonRelevant, Map<String,List<Tuple>> tuples) {
		
		Collection<List<Tuple>> values = tuples.values();
		
		List<String> ret = new ArrayList<String>();
		
		for (List<Tuple> list : values) {
			
			if (ret.size() >= numberOfQueries)
				break;
			
			for (int i = 0; i < list.size(); i++) {
				
				if (ret.size() >= numberOfQueries)
					break;
				
				Tuple t = list.get(i);
				
				Set<String> fields = t.getFieldNames();
				
				for (String field : fields) {
					
					if (ret.size() >= numberOfQueries)
						break;
						
					String quer = "+\"" + QueryParser.escape(t.getData(field).getValue()) + "\"";
					
					ret.add(quer);
					
				}
				
			}
			
		}
		
		return ret;
		
	}

}
