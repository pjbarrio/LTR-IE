package edu.columbia.cs.ltrie.updates;

import java.util.List;
import java.util.Set;

public class ConjunctionUpdateDecision implements UpdateDecision{

	private UpdateDecision[] updateDecisions;
	
	public ConjunctionUpdateDecision(UpdateDecision ... decisions){
		this.updateDecisions=decisions;
	}
	
	@Override
	public boolean doUpdate(List<String> docs, List<String> relevantDocs) {
		for(UpdateDecision particular : updateDecisions){
			if(!particular.doUpdate(docs, relevantDocs)){
				return false;
			}
		}

		return true;
	}

	@Override
	public void reset() {
		for(UpdateDecision particular : updateDecisions){
			particular.reset();
		}
	}
	
	@Override
	public String report() {
		return "";
	}
	
}
