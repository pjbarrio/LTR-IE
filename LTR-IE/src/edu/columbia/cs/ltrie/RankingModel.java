package edu.columbia.cs.ltrie;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface RankingModel {

	public RankingModel getTempCopyModel();

	public void updateModel(Collection<String> thisStepDocs, Collection<String> thisStepRelDocs, int ceil)  throws Exception;

	public Map<Long, Double> getWeightVector();

	public double getModelSimilarity(RankingModel copyModel);

}
