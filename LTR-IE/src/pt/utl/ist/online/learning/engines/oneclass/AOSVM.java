package pt.utl.ist.online.learning.engines.oneclass;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pt.utl.ist.online.learning.kernels.Kernel;
import pt.utl.ist.online.learning.utils.DataObject;

public class AOSVM<E> implements OneClassEngine<E> {
	
	private Kernel<E> kernel;
	private double lambda;
	private double C;
	private int M;
	private Map<DataObject<E>,Double> vectors = new HashMap<DataObject<E>,Double>();
	private double P;
	private Map<DataObject<E>,Double> q = new HashMap<DataObject<E>,Double>();
	private double beta=0;
	private int t=0;
	
	
	public AOSVM(double C, double lambda, Kernel<E> kernel){
		this(C, lambda,10^9,kernel);
	}
	
	public AOSVM(double C, double lambda, int M, Kernel<E> kernel){
		this.C=C;
		this.M=M;
		this.lambda=lambda;
		this.kernel=kernel;
	}

	@Override
	public boolean updateModel(DataObject<E> inputVector) {
		boolean hasUpdated;
		if(t==0){
			hasUpdated=updateFirst(inputVector);
		}else{
			hasUpdated=updateOthers(inputVector);
		}
		
		t++;
		return hasUpdated;
	}
	
	private boolean updateFirst(DataObject<E> inputVector){
		double kValue = kernel.compute(inputVector, inputVector);
		vectors.put(inputVector,kValue);
		P=1.0/kValue;
		return true;
	}
	
	private boolean updateOthers(DataObject<E> inputVector){
		double fn = getDecisionFunction(inputVector)-1.0;
		double en = -fn;
		double an;
		if(en<0){
			an=0;
		}else if(en<C/M){
			an=M;
		}else{
			an=C/en;
		}
		//q=lambda*q+2*kn*an;
		//kn = kn+gn where gn is gaussian
		//(...)
		
		return true;
	}
	
	public double getDecisionFunction(DataObject<E> vector) {
		double sum = 0;
		for(Entry<DataObject<E>,Double> entry : vectors.entrySet()){
			double kValue = kernel.compute(entry.getKey(), vector);
			sum+=kValue*entry.getValue();
		}
		
		return sum;
	}

	@Override
	public boolean getResult(DataObject<E> vector) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void compileModel() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void objectiveReport(Map<Integer, DataObject<E>> trainingData,
			Map<Integer, Boolean> desiredLabels) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getScore(DataObject<E> vector) {
		// TODO Auto-generated method stub
		return 0;
	}

}
