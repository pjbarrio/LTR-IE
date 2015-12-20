package pt.utl.ist.online.learning.passiveagressive.rate;

public class SimplePassiveAggressiveLearningRate implements
		PassiveAggressiveLearningRate {

	@Override
	public double compute(double loss, double normSq) {
		if(normSq!=0){
			return loss/normSq;
		}else{
			return 0;
		}
	}

}
