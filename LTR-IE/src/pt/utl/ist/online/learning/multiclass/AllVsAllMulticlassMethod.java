package pt.utl.ist.online.learning.multiclass;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class AllVsAllMulticlassMethod<E,L> {

	public AllVsAllMulticlassMethod(Collection<L> labels){

		/*Map<L, Float> weights = prepareWeights(problem);

		int c = 0;
		for (L label1 : labels)
		{
			for (L label2 : labels)
			{
				if (label1.compareTo(label2) < 0)// avoid redundant pairs
				{
					final Set<P> label1Examples = examplesByLabel.get(label1);
					final Set<P> label2Examples = examplesByLabel.get(label2);

					// PERF constructing each example set explicitly sucks, especially since they'll later be rebuilt with Boolean values anyway;
					// can we make a UnionMap or something?
					Map<P, L> subExamples = new HashMap<P, L>(label1Examples.size() + label2Examples.size());

					for (P label1Example : label1Examples)
					{
						subExamples.put(label1Example, label1);
					}
					for (P label2Example : label2Examples)
					{
						subExamples.put(label2Example, label2);
					}

					// Map<P,L> subExamples = new BinaryMap<P,L>(label1Examples, label1, label2Examples, label2);

					//BinaryClassificationProblem<P> subProblem = new BinaryClassificationProblem<P>(label1Examples, label2Examples);

					BinaryClassificationProblem<L, P> subProblem =
							new BinaryClassificationProblemImpl<L, P>(problem.getLabelClass(), subExamples);

					//** Unbalanced data: see prepareWeights
					final BinaryModel<L, P> binaryModel =
							binarySvm.train(subProblem, weights.get(label1), weights.get(label2));

					model.putOneVsOneModel(label1, label2, binaryModel);
					c++;
				}
			}
		}*/
	}

	/*private Map<L, Float> prepareWeights(MultiClassProblem<L, E> problem)
	{
		LabelInverter<L> labelInverter = problem.getLabelInverter();

		Map<L, Float> weights = new HashMap<L, Float>();

		//** Unbalanced data: redistribute the misclassification cost C according to
		// the numbers of examples in each class, so that each class has the same total
		// misclassification weight assigned to it and the average is param.C

		int numExamples = problem.getExamples().size();

		final Map<L, Set<E>> examplesByLabel = problem.getExamplesByLabel();

		int numClasses = examplesByLabel.size();

		// first figu
		// re out the average total C for each class if the samples were uniformly distributed
		float totalCPerClass = param.C * numExamples / numClasses;
		//float totalCPerRemainder = totalCPerClass * (numClasses - 1);


		// then assign the proper C per _sample_ within each class by distributing the per-class C
		for (Map.Entry<L, Set<E>> entry : examplesByLabel.entrySet())
		{
			L label = entry.getKey();
			Set<E> examples = entry.getValue();
			float weight = totalCPerClass / examples.size();

			weights.put(label, weight);


			//** For one-vs-all, we want the inverse class to have the same total weight as the positive class, i.e. totalCPerClass.
			//** Note scaling problem: we can't scale up the positive class, so we have to scale down the negative class
			//** i.e. we pretend that all of the negative examples are in one class, and so have totalCPerClass.

			L inverse = labelInverter.invert(label);
			int numFalseExamples = numExamples - examples.size();
			float inverseWeight = totalCPerClass / numFalseExamples;
			weights.put(inverse, inverseWeight);
		}

		return weights;
	}*/

}
