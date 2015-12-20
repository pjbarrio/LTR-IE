package edu.columbia.cs.ltrie.content.shift.svm;

import java.util.Iterator;
import java.util.Map;

import edu.berkeley.compbio.jlibsvm.oneclass.OneClassModel;
import edu.berkeley.compbio.jlibsvm.util.SparseVector;

public class MyOneClassModel<L, P> extends OneClassModel<L, P> {

	L label;
	
	public SparseVector[] mySVs;	
	public L getLabel()
	{
		return label;
	}
	
	/**
	 * Remove vectors whose alpha is zero, leaving only support vectors
	 */
	public void compact()
		{
		// do this first so as to make the arrays the right size below
		for (Iterator<Map.Entry<P, Double>> i = supportVectors.entrySet().iterator(); i.hasNext();)
			{
			Map.Entry<P, Double> entry = i.next();
			if (entry.getValue() == 0)
				{
				i.remove();
				}
			}


		// put the keys and values in parallel arrays, to free memory and maybe make things a bit faster (?)

		numSVs = supportVectors.size();
		SVs = (P[]) new Object[numSVs];
		mySVs = new SparseVector[numSVs];
		alphas = new double[numSVs];

		int c = 0;
		for (Map.Entry<P, Double> entry : supportVectors.entrySet())
			{
			SVs[c] = entry.getKey();
			mySVs[c] = (SparseVector) entry.getKey();
			alphas[c] = entry.getValue();
			c++;
			}

		supportVectors = null;
		}
	
}
