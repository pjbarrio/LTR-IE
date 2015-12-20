package pt.utl.ist.online.learning.passiveagressive.rate;

public class PAIPassiveAggressiveLearningRate implements
		PassiveAggressiveLearningRate {
	
	private double C;
	
	public PAIPassiveAggressiveLearningRate(double C){
		this.C=C;
	}

	@Override
	public double compute(double loss, double normSq) {
		return Math.min(C,loss/normSq);
	}

}
