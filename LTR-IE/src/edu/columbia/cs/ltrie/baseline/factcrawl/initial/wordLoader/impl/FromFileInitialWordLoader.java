package edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.InitialWordLoader;

public class FromFileInitialWordLoader extends InitialWordLoader {

	private String file;

	public FromFileInitialWordLoader(QueryParser qp, String file) {
		super(qp);
		this.file = file;
	}

	@Override
	protected List<String> getInitialStringQueries() throws IOException {
		
		List<String> lines = FileUtils.readLines(new File(file));
		
		List<String> ret = new ArrayList<String>(lines.size());
		
		for (int i = 0; i < lines.size(); i++) {
			
			String[] spl = lines.get(i).split(",");
			
			if (spl.length > 1){
				ret.add(spl[1]);
			}else{
				ret.add(spl[0]);
			}
			
		}
		
		return ret;
		
	}

}
