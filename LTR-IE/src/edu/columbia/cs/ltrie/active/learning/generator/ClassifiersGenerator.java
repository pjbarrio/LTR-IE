package edu.columbia.cs.ltrie.active.learning.generator;

import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.utl.ist.online.learning.LinearBinaryOnlineAlgorithm;
import pt.utl.ist.online.learning.utils.DataObject;

import com.google.gdata.util.common.base.Pair;

import edu.columbia.cs.ltrie.active.learning.creator.ClassifierCreator;
import edu.columbia.cs.ltrie.active.learning.data.Data;

public abstract class ClassifiersGenerator<C,E,I> {

	public abstract List<C> train(Data<E, I> data, ClassifierCreator<C,E> cc) throws Exception;

	public abstract List<Pair<Set<Long>,C>> update(Data<E, I> data, ClassifierCreator<C,E> cc) throws Exception;

}
