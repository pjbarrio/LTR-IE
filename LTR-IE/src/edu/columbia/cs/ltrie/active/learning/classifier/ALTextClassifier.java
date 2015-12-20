package edu.columbia.cs.ltrie.active.learning.classifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.utl.ist.online.learning.LinearBinaryOnlineAlgorithm;
import pt.utl.ist.online.learning.utils.DataObject;


import com.google.gdata.util.common.base.Pair;

import edu.columbia.cs.ltrie.active.learning.data.Data;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public abstract class ALTextClassifier<T,D,I> {

	protected List<Pair<Set<Long>,T>> classifiers;
	
//	protected Data<D,I> data;
	
	public ALTextClassifier(){
		
	}
	
	public ALTextClassifier(List<Pair<Set<Long>,T>> classifiers/*, Data<D,I> data*/) throws Exception {
		this.classifiers = classifiers;
//		this.data = data;
	}

	public abstract Data<D,I> createDataStructure(String[] label, List<String[]>text) throws IOException, Exception;

	public abstract double getConfidenceValue(String text, String label) throws Exception;

	public abstract Map<String, Double> getScores(Set<String> collection, IndexConnector conn, boolean independent) throws IOException;

	public abstract ALTextClassifier<T,D,I> copy();

	public List<T> getClassifiers(){
		
		List<T> ret = new ArrayList<T>(classifiers.size());
		
		for (Pair<Set<Long>,T> pair : classifiers) {
			ret.add(pair.getSecond());
		}
		
		return ret;
	}

//	public abstract Map<pt.utl.ist.online.learning.utils.Pair<String, String>, Double> getTermWeights(double divisor);

}
