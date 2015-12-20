package edu.columbia.cs.ltrie.active.learning.generator.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pt.utl.ist.online.learning.LinearBinaryOnlineAlgorithm;

import com.google.gdata.util.common.base.Pair;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import edu.columbia.cs.ltrie.active.learning.creator.ClassifierCreator;
import edu.columbia.cs.ltrie.active.learning.data.Data;
import edu.columbia.cs.ltrie.active.learning.generator.ClassifiersGenerator;


public class DifferentDataClassifierGenerator<C, D,I> extends
		ClassifiersGenerator<C, D,I> {

	private int numberOfClassifiers;
	private int numberOfFeatures;
	List<C> classifiers;

	public DifferentDataClassifierGenerator(int numberOfClassifieres, int numberOfFeatures){
		this.numberOfClassifiers = numberOfClassifieres;
		this.numberOfFeatures = numberOfFeatures;
		classifiers = null;
	}
	
	public DifferentDataClassifierGenerator(
			int numberOfClassifieres,
			int numberOfFeatures,
			List<C> copyOfclassifiers) {
		
		this.numberOfClassifiers = numberOfClassifieres;
		this.numberOfFeatures = numberOfFeatures;
		
		this.classifiers = copyOfclassifiers;
		
	}

	@Override
	public List<C> train(Data<D,I> data,
			ClassifierCreator<C, D> cc) throws Exception {
		
		data.select(numberOfFeatures);
		
		List<Data<D,I>> instances = data.split(numberOfClassifiers);
		
		classifiers = new ArrayList<C>();
		
		for (int i = 0; i < numberOfClassifiers; i++) {
			
			C c = cc.createClassifier(instances.get(i).getInstances());
			if (c != null)
				classifiers.add(c);
		}
		
		return classifiers;
		
	}

	@Override
	public List<Pair<Set<Long>,C>> update(Data<D,I> data,
			ClassifierCreator<C, D> cc) throws Exception {
		
		List<Data<D,I>> instances = data.split(numberOfClassifiers);
		
		List<Pair<Set<Long>,C>> ret = new ArrayList<Pair<Set<Long>,C>>();
		
		int i = 0;
		
		for (;i < classifiers.size();i++) {
			Set<Long> s = cc.updateClassifier(classifiers.get(i),instances.get(i).getInstances());
			ret.add(new Pair<Set<Long>, C>(s, classifiers.get(i)));
		}
		
		for (; i < numberOfClassifiers; i++) {
			C c = cc.createClassifier(instances.get(i).getInstances());
			if (c != null)
				classifiers.add(c);

		}
		
		return ret;
		
	}

	
}
