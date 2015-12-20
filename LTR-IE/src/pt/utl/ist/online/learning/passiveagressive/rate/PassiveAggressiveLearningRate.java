package pt.utl.ist.online.learning.passiveagressive.rate;

public interface PassiveAggressiveLearningRate {
	public double compute(double loss, double norm);
}
