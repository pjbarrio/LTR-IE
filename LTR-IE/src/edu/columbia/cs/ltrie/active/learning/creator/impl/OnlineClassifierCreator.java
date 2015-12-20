package edu.columbia.cs.ltrie.active.learning.creator.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gdata.util.common.base.Pair;

import pt.utl.ist.online.learning.BinaryOnlineAlgorithm;
import pt.utl.ist.online.learning.LinearBinaryOnlineAlgorithm;
import pt.utl.ist.online.learning.engines.ElasticNetLinearPegasosEngine;
import pt.utl.ist.online.learning.engines.L1LinearPegasosEngine;
import pt.utl.ist.online.learning.engines.LinearOnlineEngine;
import pt.utl.ist.online.learning.engines.LinearPegasosEngine;
import pt.utl.ist.online.learning.engines.LinearPerceptronEngine;
import pt.utl.ist.online.learning.engines.OnlineEngine;
import pt.utl.ist.online.learning.utils.DataObject;
import edu.columbia.cs.ltrie.active.learning.creator.ClassifierCreator;

public class OnlineClassifierCreator
		extends
		ClassifierCreator<LinearBinaryOnlineAlgorithm<Long>, Pair<Map<Integer, DataObject<Map<Long, Double>>>,Map<Integer, Boolean>>> {

	private int numEpochs;
	private String regularization;
	private double lambda;

	public OnlineClassifierCreator(int numEpochs, String regularization, double lambda){
		this.numEpochs = numEpochs;
		this.regularization = regularization;
		this.lambda = lambda;
	}
	
	@Override
	public LinearBinaryOnlineAlgorithm<Long> createClassifier(
			Pair<Map<Integer, DataObject<Map<Long, Double>>>,Map<Integer, Boolean>> cleanData) throws Exception {

		if (cleanData.getSecond().isEmpty())
			return null;
		
		LinearOnlineEngine<Long> engine;
		
		double hingeParameter = 1.0;
		
		if (regularization.equals("L1")){
			
			engine = new ElasticNetLinearPegasosEngine<Long>(lambda, 0.99, hingeParameter, true);

		}else{
			
			engine = new LinearPegasosEngine<Long>(lambda, hingeParameter);
			
		}
		
		return new LinearBinaryOnlineAlgorithm<Long>(cleanData.getFirst(), cleanData.getSecond(), numEpochs, engine);
				
	}

	@Override
	public Set<Long> updateClassifier(
			LinearBinaryOnlineAlgorithm<Long> classifier,
			Pair<Map<Integer, DataObject<Map<Long, Double>>>, Map<Integer, Boolean>> cleanData)
			throws Exception {
		
		Map<Integer, DataObject<Map<Long, Double>>> dat = cleanData.getFirst();
		Map<Integer, Boolean> cl = cleanData.getSecond();
		
		
		for (Entry<Integer, Boolean> entry : cl.entrySet()) {
			
			classifier.addExampleAndUpdate(dat.get(entry.getKey()), entry.getValue());
			
		}
		
		Set<Long> ret = new HashSet<Long>();
		/*System.err.println("In OnlineClassifierCretor, I am not updating the features that changed weights.");
		
		Map<Long,Double> c = new HashMap<Long, Double>(classifier.getWeightVectors());
		
		Map<Long,Double> cu = classifier.getWeightVectors();
		
		for (Entry<Long,Double> featW : cu.entrySet()) {
			
			Double n = c.get(featW.getKey());
			
			if (n == null || n != featW.getValue().doubleValue()){
				ret.add(featW.getKey());
			}
			
		}
		*/
		return ret;
		
	}

}
