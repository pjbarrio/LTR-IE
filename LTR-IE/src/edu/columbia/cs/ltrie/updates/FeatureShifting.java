package edu.columbia.cs.ltrie.updates;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.lucene.queryparser.classic.ParseException;

import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameter;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterPoint.Builder;
import edu.berkeley.compbio.jlibsvm.kernel.GaussianRBFKernel;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassProblem;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassProblemImpl;
import edu.berkeley.compbio.jlibsvm.util.SparseVector;
import edu.columbia.cs.ltrie.content.shift.svm.MyOneClassModel;
import edu.columbia.cs.ltrie.content.shift.svm.MyOneClassSVC;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class FeatureShifting implements UpdateDecision {

	private static Map<String, Integer> termMap;
	private double[] dis_t;
	private double[] vals_t;
	private Map<Integer, SparseVector[]> svs;
	private Map<Integer, double[]> alphas;
	private Map<Integer, Double> rhos;
	private double previousSum;
	private KernelFunction<SparseVector> kernel;
	private MyOneClassModel<Double,SparseVector> model_t;
	private Map<Integer, double[]> vals;
	private int k;
	private double threshold;
	private IndexConnector conn;
	private int lastIndex;
	private int size;
	private float gamma;
	private List<String> lastRelDocs;
	private int startAfter;

	public FeatureShifting (List<String> relevantDocs, float gamma, int k, double threshold, IndexConnector conn, int startAfter) throws IOException, ParseException{

		initialize(relevantDocs, gamma, k, threshold, conn, startAfter);

	}

	private void initialize (List<String> relevantDocs, float gamma, int k, double threshold, IndexConnector conn, int startAfter) throws IOException, ParseException{

		this.startAfter = startAfter;

		this.gamma = gamma;

		this.conn = conn;

		this.k = k;

		this.threshold = threshold;

		kernel = new GaussianRBFKernel(gamma);

		model_t = createModel(relevantDocs,kernel,conn);

		dis_t = new double[model_t.numSVs];
		vals_t = new double[3];

		calculateValues(model_t,dis_t,vals_t,kernel);

		svs = new HashMap<Integer, SparseVector[]>();

		svs.put(1,  model_t.mySVs);

		alphas = new HashMap<Integer, double[]>();

		alphas.put(1,  model_t.alphas);

		rhos = new HashMap<Integer, Double>();

		rhos.put(1,  new Double(model_t.rho));

		vals = new HashMap<Integer, double[]>();

		vals.put(1,  vals_t);

		previousSum = 0.0;

		lastIndex = 0;

		size = 0;

	}

	@Override
	public boolean doUpdate(List<String> docs, List<String> relevantDocs) {

		//should process only the new documents

		List<String> Bunlad = docs.subList(lastIndex, docs.size());


		Map<SparseVector, Integer> exampleIds = new HashMap<SparseVector, Integer>();

		Map<SparseVector, Float> examples = new HashMap<SparseVector, Float>();

		try {

			read_problem(readContent(Bunlad,conn,termMap),exampleIds,examples);

			SparseVector[] Bunlabeled = exampleIds.keySet().toArray(new SparseVector[exampleIds.size()]);

			previousSum = calculateSSum(previousSum, Bunlabeled, svs,alphas, rhos, vals, k,kernel);



			//System.err.println("PS-" + previousSum);
			//System.err.println(1.0 - previousSum/(double)size);

			boolean ret =  (lastIndex > startAfter) && (1.0 - previousSum/(double)size) < threshold;

			if (ret){
				if (this.lastRelDocs == null)
					this.lastRelDocs = relevantDocs;
				else
					this.lastRelDocs.addAll(relevantDocs);
			}
			return ret;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(1);
		}

		return false;

	}

	private List<List<Integer>> readContent(List<String> reldocs,
			IndexConnector conn, Map<String,Integer> termMap) throws IOException, ParseException {

		List<List<Integer>> ret = new ArrayList<List<Integer>>();

		for (int i = 0; i < reldocs.size(); i++) {

			Map<String, Integer> terms = conn.getTermFrequencies(reldocs.get(i), NYTDocumentWithFields.BODY_FIELD);

			List<Integer> list = new ArrayList<Integer>(terms.size());

			for (String term : terms.keySet()) {

				Integer index = termMap.get(term);

				if (index == null){

					index = termMap.size() + 1;

					termMap.put(term, index);

				}

				list.add(index);

			}

			ret.add(list);

		}

		return ret;

	}

	@Override
	public void reset() {

		lastIndex = 0;
		previousSum = 0.0;
		size = 0;

		try {
			initialize(lastRelDocs, gamma, k, threshold, conn, startAfter);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(1);
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

	private MyOneClassModel<Double,SparseVector> createModel(List<String> relevantDocs,
			KernelFunction<SparseVector> kernel, IndexConnector conn) throws IOException, ParseException {

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

		termMap = new HashMap<String, Integer>();

		read_problem(readContent(relevantDocs,conn,termMap), exampleIds, examples);

		Double label = 1.0;

		OneClassProblem<Double, SparseVector> problem = new OneClassProblemImpl<Double, SparseVector>(examples, exampleIds, label);

		MyOneClassSVC<Double, SparseVector> svm = new MyOneClassSVC<Double, SparseVector>();

		return (MyOneClassModel<Double, SparseVector>)svm.train(problem, parameters);


	}


	private static void read_problem(List<List<Integer>> relevantDocs,
			Map<SparseVector, Integer> exampleIds,
			Map<SparseVector, Float> examples) {

		Vector<Float> vy = new Vector<Float>();
		Vector<SparseVector> vx = new Vector<SparseVector>();

		for (List<Integer> list : relevantDocs) {

			vy.addElement(1.0f);

			Collections.sort(list);

			SparseVector x = new SparseVector(list.size());

			for (int j = 0; j < list.size(); j++)
			{
				//x[j] = new svm_node();
				x.indexes[j] = list.get(j);
				x.values[j] = 1.0f;
			}

			vx.addElement(x);
		}

		for (int i = 0; i < vx.size(); i++) {

			exampleIds.put(vx.get(i), i);
			examples.put(vx.get(i), vy.get(i));

		}

	}


	private double calculateSSum(double previousSum, SparseVector[] bunlabeled, Map<Integer, SparseVector[]> svs, Map<Integer, double[]> alphas, Map<Integer, Double> rhos, Map<Integer, double[]> vals, int k, KernelFunction<SparseVector> kernel) {

		double sum = 0.0;

		for (int i = 0; i<bunlabeled.length; i++) {

			lastIndex++;

			size++;

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

	@Override
	public String report() {
		return "";
	}

}