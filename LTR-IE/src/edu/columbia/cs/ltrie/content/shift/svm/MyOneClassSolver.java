package edu.columbia.cs.ltrie.content.shift.svm;

import java.util.HashMap;
import java.util.List;

import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassModel;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassSolver;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;

public class MyOneClassSolver<L, P> extends OneClassSolver<L, P> {

	public MyOneClassSolver(List<SolutionVector<P>> solutionVectors,
			QMatrix<P> Q, float C, float eps, boolean shrinking) {
		super(solutionVectors, Q, C, eps, shrinking);
	}

	public MyOneClassModel<L, P> solve()
	{
		optimize();

		MyOneClassModel<L, P> model = new MyOneClassModel<L, P>();

		calculate_rho(model);


		model.supportVectors = new HashMap<P, Double>();
		for (SolutionVector<P> svC : allExamples)
		{
			model.supportVectors.put(svC.point, svC.alpha);
		}

		// note at this point the solution includes _all_ vectors, even if their alphas are zero

		// we can't do this yet because in the regression case there are twice as many alphas as vectors		// model.compact();

		// ** logging output disabled for now
		//logger.info("optimization finished, #iter = " + iter);

		return model;
	}
}