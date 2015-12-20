package pt.utl.ist.online.learning.sgd.rate;

public class CollinsSGDLearningRate implements SGDLearningRate {

	private double initialValue;
	private int N;
	
	public CollinsSGDLearningRate(double initial, int numDataPoints){
		this.initialValue=initial;
		this.N=numDataPoints;
	}
	
	@Override
	public double compute(long k) {
		return initialValue/(1+(k/N));
	}

}
