package edu.columbia.cs.ltrie.updates;

import java.util.List;
import java.util.Set;

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;

public interface UpdateDecision {
	public boolean doUpdate(List<String> docs, List<String> relevantDocs);
	public void reset();
	public String report();
}
