package edu.columbia.cs.ltrie.active.learning.generator.impl;

import java.nio.channels.UnsupportedAddressTypeException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.gdata.util.common.base.Pair;

import edu.columbia.cs.ltrie.active.learning.classifier.ALTextClassifier;
import edu.columbia.cs.ltrie.active.learning.creator.ClassifierCreator;
import edu.columbia.cs.ltrie.active.learning.data.Data;
import edu.columbia.cs.ltrie.active.learning.generator.ClassifiersGenerator;
import edu.columbia.cs.ltrie.active.learning.instances.cleaner.InstancesCleaner;

public class UniqueClassifierGenerator<C,E,I> extends ClassifiersGenerator<C,E,I> {

	@Override
	public List<C> train(Data<E, I> data, ClassifierCreator<C, E> cc)
			throws Exception {
		return Arrays.asList(cc.createClassifier(data.getInstances()));
	}

	@Override
	public List<Pair<Set<Long>, C>> update(Data<E, I> data,
			ClassifierCreator<C, E> cc) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}


}
