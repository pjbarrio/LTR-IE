package edu.columbia.cs.ltrie.extractor.impl.reel;


import java.util.List;
import java.util.Set;

import edu.columbia.cs.ltrie.datamodel.Span;
import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ref.model.core.structure.OperableStructure;
import edu.columbia.cs.ref.model.entity.Entity;
import edu.columbia.cs.ref.model.re.Model;

public class TupleExtractorRunnable implements Runnable {

	private Set<OperableStructure> opStruct;
	private Model relationExtractionSystemInstance;
	private List<Tuple> t;
	private String relation;
	private String docPath;

	public TupleExtractorRunnable(
			Set<OperableStructure> opStruct,
			Model relationExtractionSystemInstance,List<Tuple> tuples, String docPath, String relation){
		this.opStruct = opStruct;
		this.relationExtractionSystemInstance = relationExtractionSystemInstance;
		this.relation = relation;
		this.docPath = docPath;
		this.t = tuples;
	}

	@Override
	public void run() {
		
		synchronized (relationExtractionSystemInstance) {

			for (OperableStructure operableStructure : opStruct) {
				
				if (relationExtractionSystemInstance.predictLabel(operableStructure).contains(relation)){
					
					t.add(generateTuple(docPath, operableStructure));
					
				}
				
			}

		}	
		
	}

	private Tuple generateTuple(String docPath, OperableStructure operableStructure) {
		
		Tuple ret = new Tuple();
		
		Entity[] entities = operableStructure.getCandidateSentence().getEntities();
		
		for (int i = 0; i < entities.length; i++) {
			
			ret.setData(entities[i].getEntityType(), new Span(docPath, entities[i].getOffset(), entities[i].getOffset() + entities[i].getLength(), entities[i].getValue()));
			
		}
		
		return ret;
	}
}
