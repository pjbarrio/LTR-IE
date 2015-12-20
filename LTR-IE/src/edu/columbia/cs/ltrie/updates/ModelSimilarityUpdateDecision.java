package edu.columbia.cs.ltrie.updates;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import prototype.CIMPLE.utils.CPUTimeMeasure;

import edu.columbia.cs.ltrie.RankingModel;
import edu.columbia.cs.ltrie.online.svm.OnlineRankingModel;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations.CopulaGRAnnotation;

public class ModelSimilarityUpdateDecision implements UpdateDecision {

	private RankingModel originalModel;
	private RankingModel copyModel;
	private double angleThreshold;
	private double percentageUpdates;
	private int currentDoc=0;
	private int currentVal=0;
	private long totalTime = 0;
	private int numDetections = 0;
	
	public ModelSimilarityUpdateDecision(RankingModel originalModel, double angleThreshold,
			double percentageUpdates){
		this.originalModel=originalModel;
		this.angleThreshold=angleThreshold;
		this.percentageUpdates=percentageUpdates;
		this.copyModel=originalModel.getTempCopyModel();
	}
	
	@Override
	public boolean doUpdate(List<String> docs, List<String> relevantDocs) {
		//We should send at least two iterations (thus, we use 2.0)
		long start = CPUTimeMeasure.getCpuTime();
		if(currentVal>=2.0/percentageUpdates){
			Set<String> thisStepDocs = new HashSet<String>();
			Set<String> thisStepRelDocs = new HashSet<String>();
			for(int i=currentDoc; i<currentDoc+2.0/percentageUpdates && i<docs.size(); i++){
				thisStepDocs.add(docs.get(i));
				thisStepRelDocs.add(docs.get(i));
			}
			thisStepRelDocs.retainAll(relevantDocs);
						
			try {
				copyModel.updateModel(thisStepDocs, thisStepRelDocs, (int) Math.ceil(thisStepDocs.size()*percentageUpdates));
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			currentDoc+=2.0/percentageUpdates;
			currentVal=0;
			//double cosine = getCosineSimilarity(originalModel.getWeightVector(), copyModel.getWeightVector());
			//double angle = (180.0/Math.PI)*Math.acos(Math.min(cosine,1.0));
			//System.out.println(angle + " " + originalModel.getWeightVector().size() + " " + copyModel.getWeightVector().size());
		}else{
			currentVal++;
		}
		
		//double cosine = getCosineSimilarity(originalModel.getWeightVector(), copyModel.getWeightVector());
		double cosine = originalModel.getModelSimilarity(copyModel);
		double angle = (180.0/Math.PI)*Math.acos(Math.min(cosine,1.0));
		//System.out.println(angle);
		long end = CPUTimeMeasure.getCpuTime();
		totalTime+=(end-start);
		numDetections++;
		return angle>angleThreshold;
	}

	@Override
	public void reset() {
		copyModel=originalModel.getTempCopyModel();
		currentDoc=0;
		currentVal=0;
	}

	
	private double getCosineSimilarity(Map<Long, Double> weightVector,
			Map<Long, Double> weightVector2) {
		Set<Long> commonKeys = new HashSet<Long>(weightVector.keySet());
		commonKeys.retainAll(weightVector2.keySet());
		
		double inner = 0;
		for(Long key : commonKeys){
			double val1 = weightVector.get(key);
			double val2 = weightVector2.get(key);
			inner+=val1*val2;
		}
		
		double normSq1 = 0;
		for(Entry<Long,Double> entry : weightVector.entrySet()){
			normSq1+=entry.getValue()*entry.getValue();
		}
		
		double normSq2 = 0;
		for(Entry<Long,Double> entry : weightVector2.entrySet()){
			normSq2+=entry.getValue()*entry.getValue();
		}
		
		return inner/Math.sqrt(normSq1*normSq2);
	}

	@Override
	public String report() {
		return "" + ((double)totalTime/(double)numDetections)/1000000;
	}

}
