package edu.columbia.cs.ref.algorithm.feature.generation.impl;

import java.util.Arrays;

import edu.columbia.cs.ref.model.feature.impl.SequenceFS;

public class HashedSequenceFS extends SequenceFS<String>{

	public HashedSequenceFS(SequenceFS<String> sequence) {
		super(sequence.toArray());
	}
	
	@Override
	public int hashCode(){
		return Arrays.hashCode(this.toArray());
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj instanceof HashedSequenceFS){
			return Arrays.equals(this.toArray(), ((HashedSequenceFS) obj).toArray());
		}
		return false;
	}

}
