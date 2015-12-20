package edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.queryparser.classic.QueryParser;

public class FromWeightedFileInitialWordLoader extends InitialWordLoader {

	private String file;

	public FromWeightedFileInitialWordLoader(QueryParser qp, String file) {
		super(qp);
		this.file = file;
	}

	@Override
	protected List<String> getInitialStringQueries() throws IOException {
		
		List<String> lines = FileUtils.readLines(new File(file));
		
		List<String> ret = new ArrayList<String>(lines.size());
		
		for (int i = 0; i < lines.size(); i++) {
			ret.add("+" + lines.get(i).substring(lines.get(i).indexOf(",")+1));
		}
		
		return ret;
	}

}
