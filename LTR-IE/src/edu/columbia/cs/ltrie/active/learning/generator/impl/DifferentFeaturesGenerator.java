package edu.columbia.cs.ltrie.active.learning.generator.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.gdata.util.common.base.Pair;

import edu.columbia.cs.ltrie.active.learning.creator.ClassifierCreator;
import edu.columbia.cs.ltrie.active.learning.data.Data;
import edu.columbia.cs.ltrie.active.learning.generator.ClassifiersGenerator;

public class DifferentFeaturesGenerator<C, D,I> extends ClassifiersGenerator<C, D,I> {

	private int numberOfFeatures;
	private int numberOfClassifiers;

	public DifferentFeaturesGenerator(int numberOfClassifiers, int numberOfFeatures){
		this.numberOfClassifiers = numberOfClassifiers;
		this.numberOfFeatures = numberOfFeatures;
	}


	@Override
	public List<C> train(Data<D,I> data, ClassifierCreator<C, D> cc)
			throws Exception {
		
		List<Data<D, I>> list = data.split(numberOfClassifiers);
		
		for (int i = 0; i < list.size(); i++) {
			
			list.get(i).select(numberOfFeatures);
			
		}
		
		List<C> ret = new ArrayList<C>();
		
		for (int i = 0; i < list.size(); i++) {
			
			ret.add(cc.createClassifier(list.get(i).getInstances()));
			
		}
		
		return ret;
		
	}


	@Override
	public List<Pair<Set<Long>, C>> update(Data<D, I> data,
			ClassifierCreator<C, D> cc) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}





}
