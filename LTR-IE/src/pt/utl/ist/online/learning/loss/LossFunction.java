package pt.utl.ist.online.learning.loss;

public interface LossFunction {
	public double getLossValue(double decisionValue, int label);
	public double getLossDerivativeValue(double decisionValue, int label);
}
