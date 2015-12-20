package edu.columbia.cs.ltrie.content.shift.svm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameter;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterGrid;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterPoint;
import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassProblem;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassSVC;
import edu.berkeley.compbio.jlibsvm.qmatrix.BooleanInvertingKernelQMatrix;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;
import edu.berkeley.compbio.jlibsvm.regression.RegressionModel;


public class MyOneClassSVC<L extends Comparable, P> extends OneClassSVC<L, P> {

	public RegressionModel<P> train(OneClassProblem<L, P> problem, ImmutableSvmParameter<Float, P> param)
	//, final TreeExecutorService execService)
	{
		validateParam(param);
		RegressionModel<P> result;
		if (param instanceof ImmutableSvmParameterGrid && param.gridsearchBinaryMachinesIndependently)
		{
			throw new SvmException(
					"Can't do grid search without cross-validation, which is not implemented for regression SVMs.");
		}
		else
		{
			result = trainScaled(problem, (ImmutableSvmParameterPoint<Float, P>) param);//, execService);
		}
		return result;
	}

	private RegressionModel<P> trainScaled(OneClassProblem<L, P> problem,
			ImmutableSvmParameterPoint<Float, P> param)
			//,final TreeExecutorService execService)
			{
		if (param.scalingModelLearner != null && param.scaleBinaryMachinesIndependently)
		{
			// the examples are copied before scaling, not scaled in place
			// that way we don't need to worry that the same examples are being used in another thread, or scaled differently in different contexts, etc.
			// this may cause memory problems though

			problem = problem.getScaledCopy(param.scalingModelLearner);
		}


		float remainingAlpha = param.nu * problem.getNumExamples();

		float linearTerm = 0f;
		List<SolutionVector<P>> solutionVectors = new ArrayList<SolutionVector<P>>();
		int c = 0;
		for (Map.Entry<P, Float> example : problem.getExamples().entrySet())
		{
			float initAlpha = remainingAlpha > 1f ? 1f : remainingAlpha;
			remainingAlpha -= initAlpha;

			SolutionVector<P> sv;

			sv = new SolutionVector<P>(problem.getId(example.getKey()), example.getKey(), true, linearTerm, initAlpha);
			//sv.id = problem.getId(example.getKey());
			c++;
			solutionVectors.add(sv);
		}

		QMatrix<P> qMatrix =
				new BooleanInvertingKernelQMatrix<P>(param.kernel, problem.getNumExamples(), param.getCacheRows());
		MyOneClassSolver<L, P> s = new MyOneClassSolver<L, P>(solutionVectors, qMatrix, 1.0f, param.eps, param.shrinking);


		MyOneClassModel<L, P> model = s.solve(); //new RegressionModel<P>(binaryModel);
		//model.kernel = kernel;
		model.param = param;
		model.label = problem.getLabel();
		model.setSvmType(getSvmType());
		model.compact();

		return model;
			}

}
