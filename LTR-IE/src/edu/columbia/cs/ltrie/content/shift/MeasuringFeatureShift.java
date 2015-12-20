package edu.columbia.cs.ltrie.content.shift;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.Vector;

import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameter;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterPoint.Builder;
import edu.berkeley.compbio.jlibsvm.kernel.GaussianRBFKernel;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.kernel.LinearKernel;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassProblem;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassProblemImpl;
import edu.berkeley.compbio.jlibsvm.util.SparseVector;
import edu.columbia.cs.ltrie.content.shift.svm.MyOneClassModel;
import edu.columbia.cs.ltrie.content.shift.svm.MyOneClassSVC;

public class MeasuringFeatureShift {

	
	
	public static void main(String[] args) throws FileNotFoundException {
		
		String file_true = "BONG-NaturalDisaster-SF-CRF-model_1_5000_TRUE.libsvm";
		String file_false = "BONG-NaturalDisaster-SF-CRF-model_1_5000_TRUE.libsvm";
		
		String file_test = "BONG-NaturalDisaster-SF-CRF-model_1_5000_TRUE_TEST.libsvm";
		
		float inc = 0.002f;
		
		Map<SparseVector, Integer> exampleIds = new HashMap<SparseVector, Integer>();
		Map<SparseVector, Float> examples = new HashMap<SparseVector, Float>();

		exampleIds.clear();
		examples.clear();

		try {
			read_problem(file_test, exampleIds, examples);
		} catch (IOException e) {
			e.printStackTrace();
		}

		SparseVector[] Bunlabeled = exampleIds.keySet().toArray(new SparseVector[exampleIds.size()]);

		exampleIds.clear();
		examples.clear();
		
		for (float gamma = inc; gamma < 0.3; gamma+=inc) {
			
			System.err.println(gamma);
			
			KernelFunction<SparseVector> kernel = new GaussianRBFKernel(gamma);

			MyOneClassModel<Double,SparseVector> model_t = createModel(file_true,kernel);
//			MyOneClassModel<Double,SparseVector> model_f = createModel(file_false,kernel);
			
			
			//should repeat for Not Useful. ?
			
			double[] dis_t = new double[model_t.numSVs];
			double[] vals_t = new double[3];
			
			calculateValues(model_t,dis_t,vals_t,kernel);
			
//			double[] dis_f = new double[model_f.numSVs];
//			double[] vals_f = new double[3];
			
//			calculateValues(model_f,dis_f,vals_f,kernel);
			
			int k = 6;
			
			Map<Integer, SparseVector[]> svs = new HashMap<Integer, SparseVector[]>();
			
			svs.put(1,  model_t.mySVs);
//			svs.put(0,  model_f.mySVs);
			
			Map<Integer, double[]> alphas = new HashMap<Integer, double[]>();
			
			alphas.put(1,  model_t.alphas);
//			alphas.put(0,  model_f.alphas);
			
			Map<Integer, Double> rhos = new HashMap<Integer, Double>();
			
			rhos.put(1,  new Double(model_t.rho));
//			rhos.put(0,  new Double(model_f.rho));
			
			Map<Integer, double[]> vals = new HashMap<Integer, double[]>();
			
			vals.put(1,  vals_t);
//			vals.put(0,  vals_f);
			
			double previousSum = 0.0;
			
			System.setOut(new PrintStream(new File("featureShift/" + gamma + ".csv")));
			
			for (int i = 0; i < Bunlabeled.length; i++) {
				
				previousSum = calculateSSum(previousSum,i,i+1, Bunlabeled, svs,alphas, rhos, vals, k,kernel);
				
				System.out.println(i + "," + (1.0 - previousSum/(i+1)));
				
			}
			
		}
		
		
		


	}

	private static void calculateValues(
			MyOneClassModel<Double, SparseVector> model, double[] dis,
			double[] vals, KernelFunction<SparseVector> kernel) {
		
		for (int i = 0; i < model.numSVs; i++) {
			dis[i] = calculateDistance(model.mySVs[i],model.alphas, model.mySVs,model.rho,kernel);
		}
		
		vals[0] = Double.MAX_VALUE;
		vals[1] = 0.0;
		vals[2] = 0.0;
		
		for (int i = 0; i < dis.length; i++) {
			if (dis[i] < vals[0])
				vals[0] = dis[i];
			vals[0]+=dis[i];
		}
		
		vals[1] /= (double)dis.length;
		
		for (int i = 0; i < dis.length; i++) {
			vals[2] += ((dis[i]-vals[1])*(dis[i]-vals[2]));
		}
		
		vals[2] /= (double)dis.length;

		
	}

	private static MyOneClassModel<Double,SparseVector> createModel(String file,
			KernelFunction<SparseVector> kernel) {
		
		Builder<Float, SparseVector> builder = new Builder<Float, SparseVector>();
		
		builder.kernel = kernel;
		builder.nu = 0.5f;
		builder.cache_size = 40;
		builder.C = 1;
		builder.eps = 1e-3f;
		builder.p = 0.1f;
		builder.shrinking = true;
		builder.probability = false;
		
		ImmutableSvmParameter<Float, SparseVector> parameters = builder.build();
		
		Map<SparseVector, Integer> exampleIds = new HashMap<SparseVector, Integer>();
		Map<SparseVector, Float> examples = new HashMap<SparseVector, Float>();;
		
		try {
			read_problem(file, exampleIds, examples);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		Double label = 1.0;
		
		OneClassProblem<Double, SparseVector> problem = new OneClassProblemImpl<Double, SparseVector>(examples, exampleIds, label);
		
		MyOneClassSVC<Double, SparseVector> svm = new MyOneClassSVC<Double, SparseVector>();
				
		return (MyOneClassModel<Double, SparseVector>)svm.train(problem, parameters);

		
	}

	private static double calculateSSum(double previousSum, int firstIndex, int size, SparseVector[] bunlabeled, Map<Integer, SparseVector[]> svs, Map<Integer, double[]> alphas, Map<Integer, Double> rhos, Map<Integer, double[]> vals, int k, KernelFunction<SparseVector> kernel) {
		
		double sum = 0.0;
		
		for (int i = firstIndex; i < size; i++) {
			
			sum+= getMax(bunlabeled[i],svs,alphas, rhos,vals,kernel,k);
			
		}
		
		return previousSum + sum;

	}


	private static double getMax(SparseVector sparseVector,
			Map<Integer, SparseVector[]> svs, Map<Integer, double[]> alphas, Map<Integer, Double> rhos, Map<Integer, double[]> vals,
			KernelFunction<SparseVector> kernel, int k) {
		
		double max = Double.MIN_VALUE;
		
		for (Entry<Integer,SparseVector[]> entry : svs.entrySet()) {
			
			double[] val = vals.get(entry.getKey());
			
			double aux = calculatesmallS(sparseVector,kernel, entry.getValue(), alphas.get(entry.getKey()), rhos.get(entry.getKey()), val[0],val[1],val[2],k);
			
			if (aux>max){
				max = aux;
			}
			
		}
		
		return max;
		
	}

	private static double calculatesmallS(SparseVector sparseVector,
			KernelFunction<SparseVector> kernel, SparseVector[] value, double[] alphas, double rho,
			double min, double mean, double variance, double k) {
		
		double d = calculateDistance(sparseVector, alphas, value, rho, kernel);
		
		if (d <= min)
			return 1;
		
		double aux = mean + k*Math.sqrt(variance);
		
		if (d >= (aux))
			return 0;
		
		return (aux - d)/(aux - min);
		
	}

	private static double calculateDistance(SparseVector s1, double[] alphas,
			SparseVector[] mySVs, double rho, KernelFunction<SparseVector> kernel) {
		
		double sum = 0.0;
		
		for (int i = 0; i < mySVs.length; i++) {
			
			sum += alphas[i]*kernel.evaluate(mySVs[i], s1);
			
		}
		
		return sum*(-1) + rho;
	}

	private static void read_problem(String file, Map<SparseVector, Integer> exampleIds, Map<SparseVector, Float> examples) throws IOException {
		
		BufferedReader fp = new BufferedReader(new FileReader(file));
		Vector<Float> vy = new Vector<Float>();
		Vector<SparseVector> vx = new Vector<SparseVector>();
		int max_index = 0;

		while (true)
			{
			String line = fp.readLine();
			if (line == null)
				{
				break;
				}

			StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");

			vy.addElement(Float.parseFloat(st.nextToken()));
			int m = st.countTokens() / 2;
			SparseVector x = new SparseVector(m);
			for (int j = 0; j < m; j++)
				{
				//x[j] = new svm_node();
				x.indexes[j] = Integer.parseInt(st.nextToken());
				x.values[j] = Float.parseFloat(st.nextToken());
				}
			if (m > 0)
				{
				max_index = Math.max(max_index, x.indexes[m - 1]);
				}
			vx.addElement(x);
			}
		
		fp.close();
		
		for (int i = 0; i < vx.size(); i++) {
			
			exampleIds.put(vx.get(i), i);
			examples.put(vx.get(i), vy.get(i));
			
		}
	}
	
	
}
