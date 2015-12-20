package pt.utl.ist.online.learning.sgd.rate;

public class UniformLearningRate implements SGDLearningRate {

	private double initialValue;
	
	public UniformLearningRate(double initial){
		this.initialValue=initial;
	}
	
	@Override
	public double compute(long k) {
		return initialValue;
	}

}
