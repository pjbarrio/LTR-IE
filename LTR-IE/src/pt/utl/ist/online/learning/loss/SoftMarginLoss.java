package pt.utl.ist.online.learning.loss;

public class SoftMarginLoss implements LossFunction {

	private double threshold;
	
	public SoftMarginLoss(double threshold){
		this.threshold=threshold;
	}
	
	@Override
	public double getLossValue(double decisionValue, int label) {
		return Math.max(0, threshold-label*decisionValue);
	}

	@Override
	public double getLossDerivativeValue(double decisionValue, int label) {
		if(label*decisionValue<=threshold){
			return -label;
		}else{
			return 0;
		}
	}

}
