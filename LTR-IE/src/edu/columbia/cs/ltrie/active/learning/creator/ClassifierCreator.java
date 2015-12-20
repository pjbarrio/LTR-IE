package edu.columbia.cs.ltrie.active.learning.creator;

import java.util.Set;

import edu.columbia.cs.ltrie.active.learning.data.Data;

public abstract class ClassifierCreator<C,D> {

	public abstract C createClassifier(D cleanData) throws Exception;

	public abstract Set<Long> updateClassifier(C classifier, D cleanData) throws Exception;
	
}
