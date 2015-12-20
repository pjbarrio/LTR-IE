package edu.columbia.cs.ltrie.indexing;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.util.BytesRef;

public class SimpleFrequencySimilarity extends DefaultSimilarity {
	public SimpleFrequencySimilarity(){
	}
	
	@Override
	public float decodeNormValue(byte b){
		return 1.0f;
	}
	
	@Override
	public float idf(long docFreq, long numDocs) {
		return(float)1.0;
	}
	
	@Override
	public float coord(int overlap, int maxOverlap) {
		return 1.0f;
	}
	
	@Override
	public float lengthNorm(FieldInvertState state) {
		return 1.0f;
	}
	
	@Override
	public float queryNorm(float sumOfSquaredWeights){
		return 1.0f;
	}
	
	@Override
	public float scorePayload(int doc, int start, int end, BytesRef payload){
		return 1.0f;
	}
	
	@Override
	public float sloppyFreq(int distance){
		return 1.0f;
	}
	
	@Override
	public float tf(float freq){
		return 1.0f;
	}
	
	@Override
	public float tf(int freq){
		return freq;
	}
}
