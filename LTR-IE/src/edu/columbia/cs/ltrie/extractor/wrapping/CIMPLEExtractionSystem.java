package edu.columbia.cs.ltrie.extractor.wrapping;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import edu.columbia.cs.ltrie.datamodel.Span;
import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.extractor.ExtractionSystem;
import edu.columbia.cs.ltrie.utils.SerializationHelper;

import prototype.CIMPLE.execution.OperatorNode;
import prototype.CIMPLE.loader.DocumentContentLoader;
import prototype.CIMPLE.loader.ListOfFilesLoader;
import prototype.CIMPLE.optimizer.Optimizer;

public class CIMPLEExtractionSystem extends ExtractionSystem {
	private OperatorNode executionPlan;
	
	public CIMPLEExtractionSystem(String executionPlan) throws IllegalArgumentException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException{
		this.executionPlan = (OperatorNode) SerializationHelper.read(executionPlan);
	}

	@Override
	public List<Tuple> execute(String path, String docContent) {
		executionPlan.resetNode();
		DocumentContentLoader oneDocLoader = new DocumentContentLoader(path,docContent);
		oneDocLoader.loadFullCollection();
		List<prototype.CIMPLE.datamodel.Tuple> resultsCIMPLE = executionPlan.execute();
		List<Tuple> results = new ArrayList<Tuple>();
		for(prototype.CIMPLE.datamodel.Tuple t : resultsCIMPLE){
			Tuple newTuple = new Tuple();
			for(int i=0;i<t.getSize();i++){
				prototype.CIMPLE.datamodel.Span cimpleSpan = (prototype.CIMPLE.datamodel.Span)t.getData(i);
				Span newSpan = new Span(path, cimpleSpan.getStart(), cimpleSpan.getEnd(), cimpleSpan.getValue());
				newTuple.setData("attribute" + i, newSpan);
			}
			results.add(newTuple);
		}
		return results;
	}

	@Override
	public String getPlanString() {
		return executionPlan.toString();
	}

}
