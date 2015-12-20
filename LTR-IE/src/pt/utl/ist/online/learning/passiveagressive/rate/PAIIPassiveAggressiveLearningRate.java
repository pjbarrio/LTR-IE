package pt.utl.ist.online.learning.passiveagressive.rate;

public class PAIIPassiveAggressiveLearningRate implements
		PassiveAggressiveLearningRate {
	
	private double oneDividedByTwoC;
	
	public PAIIPassiveAggressiveLearningRate(double C){
		this.oneDividedByTwoC=1/(2*C);
	}

	@Override
	public double compute(double loss, double normSq) {
		return loss/(normSq+oneDividedByTwoC);
	}

}
