package edu.columbia.cs.ltrie.active.learning.data.impl;

import java.util.HashMap;
import java.util.Map;

import pt.utl.ist.online.learning.utils.DataObject;

import com.google.gdata.util.common.base.Pair;

import edu.columbia.cs.ltrie.active.learning.data.Data;

public class OnlineLearningData
		extends
		Data<Pair<Map<Integer, DataObject<Map<Long, Double>>>, Map<Integer, Boolean>>, Pair<DataObject<Map<Long, Double>>,Boolean>> {

	
	
	public OnlineLearningData(
			Pair<Map<Integer, DataObject<Map<Long, Double>>>, Map<Integer, Boolean>> data) {
		super(data);
	}

	@Override
	public Data<Pair<Map<Integer, DataObject<Map<Long, Double>>>, Map<Integer, Boolean>>, Pair<DataObject<Map<Long, Double>>,Boolean>> select(
			int numOfFeatures) throws Exception {
		return null;
	}

	@Override
	public Data<Pair<Map<Integer, DataObject<Map<Long, Double>>>, Map<Integer, Boolean>>, Pair<DataObject<Map<Long, Double>>,Boolean>> createNewInstance() {
		return new OnlineLearningData(new Pair<Map<Integer,DataObject<Map<Long,Double>>>, Map<Integer,Boolean>>(new HashMap<Integer,DataObject<Map<Long,Double>>>(), new HashMap<Integer, Boolean>()));
	}

	@Override
	public int size() {
		return getInstances().getSecond().size();
	}

	@Override
	public Pair<DataObject<Map<Long, Double>>,Boolean> get(int i) {
		return new Pair<DataObject<Map<Long,Double>>, Boolean>(getInstances().getFirst().get(i), getInstances().getSecond().get(i));
	}

	@Override
	public double getClassValue(Pair<DataObject<Map<Long, Double>>,Boolean> instance) {
		if (instance.second.booleanValue())
			return 1.0;
		return 0.0;
	}

	@Override
	public void addInstance(Pair<DataObject<Map<Long, Double>>,Boolean> instance) {
		
		int ind = size();
		
		getInstances().getFirst().put(ind, instance.getFirst());
		getInstances().getSecond().put(ind, instance.getSecond());

	}

}
