package pt.utl.ist.online.learning.engines;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.kernels.Kernel;
import pt.utl.ist.online.learning.utils.DataObject;


public class KernelElasticPegasosEngine<E> implements OnlineEngine<E>, Serializable {
	
	private Map<DataObject<E>,Double> weights;
	private Map<DataObject<E>,Double> ys;
	private double b=0.0;
	private double lambda;
	private double importanceL2;
	private double hingeParameter;
	private Kernel<E> kernel;
	private double t=0;
	private double currentNorm=0.0;
	private double currentProd=0.0;
	private Map<DataObject<E>,Double> indivProd;
	
	public KernelElasticPegasosEngine(double lambda, double importanceL2, double hingeParameter, Kernel<E> k) {
		this.lambda=lambda;
		weights = new HashMap<DataObject<E>, Double>();
		ys = new HashMap<DataObject<E>, Double>();
		indivProd = new HashMap<DataObject<E>, Double>();
		this.kernel=k;
		this.hingeParameter=hingeParameter;
		this.importanceL2=importanceL2;
	}

	public boolean updateModel(DataObject<E> inputVector, boolean desiredOutput){
		t++;
		double y=1.0;
		if(!desiredOutput){
			y=-1.0;
		}
		
		Map<DataObject<E>,Double> kernels = getKernels(inputVector);
		double inner = decisionFunction(kernels);
		reWeight(1.0-(1/t)*(importanceL2));
		
		if(y*(inner-b)<=hingeParameter){
			Double alpha = weights.get(inputVector);
			double normSq = kernel.compute(inputVector, inputVector);
			if(alpha==null){
				alpha=0.0;
				
				double myProd=0.0;
				for(Entry<DataObject<E>,Double> supportVector : weights.entrySet()){
					DataObject<E> sv = supportVector.getKey();
					Double prod = indivProd.get(sv);
					if(prod==null){
						prod=0.0;
					}
					double ySv = ys.get(sv);
					Double kOther = kernels.get(sv);
					if(kOther==null){
						kOther=0.0;
					}
					double diff = ySv*y*kOther;
					prod+=diff;
					myProd+=diff;
					currentProd+=2*diff;
					indivProd.put(sv, prod);
				}
				
				double diff = normSq;
				myProd+=diff;
				currentProd+=diff;
				indivProd.put(inputVector, myProd);
			}
			alpha+=1.0/(lambda*t);
			b-=y/(t*lambda);
			weights.put(inputVector, alpha);
			ys.put(inputVector, y);
			double diff = y/(t*lambda);
			currentNorm+=(2*diff*inner+Math.pow(diff, 2)*normSq);
		}
		
		double updateK = (1.0/t)*(1.0-importanceL2);
		HashMap<DataObject<E>,Double> toRemoveDiffs = new HashMap<DataObject<E>,Double>();
		currentNorm+=updateK*updateK*currentProd;
		HashMap<DataObject<E>,Double> newWeights = new HashMap<DataObject<E>, Double>();
		for(Entry<DataObject<E>,Double> supportVector : weights.entrySet()){
			double alpha = supportVector.getValue();
			currentNorm-=2*alpha*updateK*indivProd.get(supportVector.getKey());
			double diff = Math.min(alpha, updateK);
			alpha-=diff;
			
			if(alpha==0){
				toRemoveDiffs.put(supportVector.getKey(),updateK-diff);
			}else{
				newWeights.put(supportVector.getKey(), alpha);
			}
		}
			
		boolean toBreak=false;
		for(Entry<DataObject<E>,Double> d : toRemoveDiffs.entrySet()){
			DataObject<E> sv = d.getKey();
			Map<DataObject<E>,Double> kernelsSv = getKernels(sv);
			double innerSv = decisionFunction(kernelsSv);
			double diff = d.getValue();
			double myY = ys.get(sv);
			double myProd = indivProd.get(sv);
			currentNorm+=2*innerSv*diff*myY;
			currentNorm-=2*updateK*diff*myProd;
			for(Entry<DataObject<E>,Double> dOther : toRemoveDiffs.entrySet()){
				DataObject<E> otherSv = dOther.getKey();
				Double kOther = kernelsSv.get(otherSv);
				if(kOther==null){
					kOther=0.0;
				}
				double yOther = ys.get(otherSv);
				double diffOther = dOther.getValue();
				currentNorm+=diff*diffOther*yOther*myY*kOther;
			}
			for(Entry<DataObject<E>,Double> dOther : indivProd.entrySet()){
				DataObject<E> otherSv = dOther.getKey();
				if(!toRemoveDiffs.containsKey(otherSv)){
					double otherProd = dOther.getValue();
					Double kOther = kernelsSv.get(otherSv);
					if(kOther==null){
						kOther=0.0;
					}
					double yOther = ys.get(otherSv);
					otherProd-=myY*yOther*kOther;
					currentProd-=myY*yOther*kOther;
					indivProd.put(otherSv, otherProd);
				}
			}
			indivProd.remove(sv);
			currentProd-=myProd;
			//toBreak=true;
			//System.exit(1);
		}
		
		weights=newWeights;
		if(weights.size()==0){
			currentNorm=0;
			currentProd=0;
		}
		
		//double normSq=normSq();
		double normSq=currentNorm;
		//System.out.println(normSq + " " + currentNorm + " " + currentProd + " " + indivProd.size());
		
		reWeight(Math.min(1.0, 1.0/(Math.sqrt(normSq*lambda))));
		return true;
	}

	private double normSq() {
		ArrayList<DataObject<E>> supportVectors = new ArrayList<DataObject<E>>(weights.keySet());
		int svSize = supportVectors.size();
		double sum=0.0;
		for(int i=0; i<svSize; i++){
			DataObject<E> svI = supportVectors.get(i);
			double alphaI = weights.get(svI);
			double normI = kernel.compute(svI, svI);
			double yI = ys.get(svI);
			sum+=alphaI*alphaI*normI;
			for(int j=i+1; j<svSize; j++){
				DataObject<E> svJ = supportVectors.get(j);
				double alphaJ = weights.get(svJ);
				double yJ = ys.get(svJ);
				double k = kernel.compute(svI, svJ);
				sum+=2*alphaI*alphaJ*k*yI*yJ;
			}
		}
		return sum;
	}

	private void reWeight(double d) {
		currentNorm*=(d*d);
		for(Entry<DataObject<E>,Double> supportVector : weights.entrySet()){
			DataObject<E> sv = supportVector.getKey();
			weights.put(sv,supportVector.getValue()*d);
		}
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
		for(Entry<DataObject<E>,Double> supportVector : weights.entrySet()){
			DataObject<E> sv = supportVector.getKey();
			double weight = supportVector.getValue();
			double y = ys.get(sv);
			
			sum+= weight*kernel.compute(sv, x)*y;
		}
		return sum;
	}
	
	private double decisionFunction(Map<DataObject<E>,Double> kernels){
		double sum = 0.0;
		for(Entry<DataObject<E>,Double> supportVector : kernels.entrySet()){
			DataObject<E> sv = supportVector.getKey();
			double kValue = supportVector.getValue();
			double weight = weights.get(sv);
			double y = ys.get(sv);
			sum+= weight*kValue*y;
		}
		return sum;
	}
	
	private Map<DataObject<E>,Double> getKernels(DataObject<E> x){
		Map<DataObject<E>,Double> kernels = new HashMap<DataObject<E>, Double>();
		for(Entry<DataObject<E>,Double> supportVector : weights.entrySet()){
			DataObject<E> sv = supportVector.getKey();
			double kValue = kernel.compute(sv, x);
			if(kValue!=0){
				kernels.put(sv, kValue);
			}
		}
		return kernels;
	}
	
	public DataObject<E> convertVector(DataObject<E> x) throws InvalidVectorIndexException{
		return x;
	}

	public int numSVs() {
		return weights.size(); 
	}

	@Override
	public void objectiveReport(Map<Integer, DataObject<E>> trainingData, Map<Integer, Boolean> desiredLabels) {
		/*System.out.println("L2(w): " + currentNorm);
		double sum = 0.0;
		for(double d : weights.values()){
			sum+=d;
		}
		System.out.println("L1(alphas): " + sum);*/
		
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

}
