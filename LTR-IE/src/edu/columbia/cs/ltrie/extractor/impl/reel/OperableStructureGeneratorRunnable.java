package edu.columbia.cs.ltrie.extractor.impl.reel;


import java.util.Arrays;
import java.util.Set;

import edu.columbia.cs.ref.model.CandidateSentence;
import edu.columbia.cs.ref.model.StructureConfiguration;
import edu.columbia.cs.ref.model.core.structure.OperableStructure;

public class OperableStructureGeneratorRunnable implements Runnable {

	private StructureConfiguration structureConfiguration;
	private Set<CandidateSentence> candidateSenteces;
	private Set<OperableStructure> result;

	public OperableStructureGeneratorRunnable(Set<CandidateSentence> candidateSentences,
			StructureConfiguration structureConfiguration, Set<OperableStructure> result) {
		this.candidateSenteces = candidateSentences;
		this.structureConfiguration = structureConfiguration;
		this.result = result;
		
	} 

	@Override
	public void run() {
		
		if (candidateSenteces.isEmpty()){
			
			return;
			
		}
		
		int i=1;
		System.out.println(candidateSenteces.size());
		for(CandidateSentence sent : candidateSenteces){
			result.add(structureConfiguration.getOperableStructure(sent));
			i++;
		}
		
	}

}
