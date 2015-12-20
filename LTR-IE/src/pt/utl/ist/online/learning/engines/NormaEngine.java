package pt.utl.ist.online.learning.engines;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.kernels.Kernel;
import pt.utl.ist.online.learning.loss.LossFunction;
import pt.utl.ist.online.learning.utils.DataObject;


public class NormaEngine<E> implements OnlineEngine<E>, Serializable {
	
	private double learningRate;
	private Kernel<E> kernel;
	private LossFunction loss;
	
	private int t;
	private double[] betas;
	private ArrayList<Double> alphas;
	private ArrayList<DataObject<E>> supportVectors;
	private double b=0.0;
	private int truncationParameter;
	private double C;

	public NormaEngine(double regularizationParameter, int truncationParameter, double learningRate, LossFunction loss, Kernel<E> k) {
		this.t=0;
		this.truncationParameter=truncationParameter;
		betas = new double[truncationParameter+1];
		for(int i=0; i<truncationParameter+1; i++){
			betas[i]=Math.pow((1-regularizationParameter*learningRate), i);
		}
		this.C=regularizationParameter;
		this.learningRate=learningRate;
		this.kernel=k;
		this.loss=loss;
		alphas= new ArrayList<Double>();
		supportVectors = new ArrayList<DataObject<E>>();
	}

	public boolean updateModel(DataObject<E> inputVector, boolean desiredOutput){
		double ft = decisionFunction(inputVector);
		int label;
		if(desiredOutput){
			label=1;
		}else{
			label=-1;
		}
		double alphaT = -learningRate*loss.getLossDerivativeValue(ft, label);
		if(label*(ft-b)<=1.0){
			double diff=label*learningRate; 
			b-=diff;
		}
		
		alphas.add(alphaT);
		supportVectors.add(inputVector);
		t++;
		return true;
	}
	
	public boolean getResult(DataObject<E> vector){
		int currentResult = (int) Math.signum(decisionFunction(vector)-b);
		if(currentResult>0){
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public double getScore(DataObject<E> vector) {
		return decisionFunction(vector)-b;
	}
	
	private double decisionFunction(DataObject<E> x){
		double sum = 0.0;
		for(int i=Math.max(0, t-truncationParameter);i<t; i++){
			double alpha = alphas.get(i);
			double beta = betas[t-i];
			double k = kernel.compute(supportVectors.get(i), x);
			sum+=alpha*beta*k;
		}
		return sum;
	}
	
	public DataObject<E> convertVector(DataObject<E> x) throws InvalidVectorIndexException{
		return x;
	}
	
	@Override
	public void compileModel() {
		kernel.clear();
		kernel.setUseCache(false);
	}

	@Override
	public void start() {
		kernel.setUseCache(true);
	}

	@Override
	public void objectiveReport(Map<Integer, DataObject<E>> trainingData, Map<Integer, Boolean> desiredLabels) {
		double l1Norm=l1NormWeights();
		double loss = 0;
		double sum = sumWeights();
		int numCorrect=0;
		int numTotal=0;
		for(Entry<Integer, DataObject<E>> entry :trainingData.entrySet()){
			boolean desiredOutput = desiredLabels.get(entry.getKey());
			DataObject<E> instance = entry.getValue();
			double f = decisionFunction(instance);
			boolean obtainedOutput = f>0;
			double innerValue=f;
			if(!desiredOutput){
				innerValue=-innerValue;
			}
			loss+=Math.max(0, 1.0-innerValue);
			if(desiredOutput==obtainedOutput){
				numCorrect++;
			}
			numTotal++;
			
			//System.out.println(f + " " + desiredOutput);
		}
		
		int numPositives=0;
		int numNegatives=0;
		for(int i=Math.max(0, t-truncationParameter);i<t; i++){
			double alpha = alphas.get(i)*betas[t-i-1];
			if(alpha>0){
				numPositives++;
			}else if(alpha<0){
				numNegatives++;
			}
		}
		
		//estas mudanças repentinas na loss são muito estranhas porque não há grandes variações nos weights... investigar isto!!!
		
		System.out.println("Accuracy: " + ((double)numCorrect/(double)numTotal));
		System.out.println("Size weights: " + truncationParameter);
		System.out.println("Sum weights: "  + sum);
		System.out.println("Num positive weights: " + numPositives);
		System.out.println("Num negative weights: " + numNegatives);
		System.out.println("L2-Norm:"  + l2NormWeights());
		System.out.println("L1-Norm:"  + l1Norm);
		System.out.println("Loss:" + loss);
		System.out.println("Objective(" + C + "):" + ((C/2)*Math.pow(l2NormWeights(),2)+(loss)));
	}

	private double sumWeights() {
		double sum=0.0;
		
		for(int i=Math.max(0, t-truncationParameter);i<t; i++){
			double alpha = alphas.get(i)*betas[t-i-1];
			sum+=alpha;
		}
		
		return sum;
	}

	private double l1NormWeights() {
		double sum=0.0;
		
		for(int i=Math.max(0, t-truncationParameter);i<t; i++){
			double alpha = alphas.get(i)*betas[t-i-1];
			sum+=Math.abs(alpha);
		}
		
		return sum;
	}
	
	private double l2NormWeights() {
		double sum=0.0;
		
		for(int i=Math.max(0, t-truncationParameter);i<t; i++){
			double alpha = alphas.get(i)*betas[t-i-1];
			sum+=Math.pow(alpha,2);
		}
		
		return Math.sqrt(sum);
	}

}
