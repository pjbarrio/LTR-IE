package edu.columbia.cs.ltrie.active.learning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import pt.utl.ist.online.learning.LinearBinaryOnlineAlgorithm;
import pt.utl.ist.online.learning.utils.DataObject;

import com.google.gdata.util.common.base.Pair;

import edu.columbia.cs.ltrie.RankingModel;
import edu.columbia.cs.ltrie.active.learning.classifier.ALTextClassifier;
import edu.columbia.cs.ltrie.active.learning.classifier.impl.OnlineClassifier;
import edu.columbia.cs.ltrie.active.learning.classifier.util.Combiner;
import edu.columbia.cs.ltrie.active.learning.classifier.util.impl.MaxCombiner;
import edu.columbia.cs.ltrie.active.learning.classifier.util.impl.SumCombiner;
import edu.columbia.cs.ltrie.active.learning.creator.ClassifierCreator;
import edu.columbia.cs.ltrie.active.learning.creator.impl.OnlineClassifierCreator;
import edu.columbia.cs.ltrie.active.learning.data.Data;
import edu.columbia.cs.ltrie.active.learning.generator.ClassifiersGenerator;
import edu.columbia.cs.ltrie.active.learning.generator.impl.DifferentDataClassifierGenerator;
import edu.columbia.cs.ltrie.features.FeaturesCoordinator;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class ActiveLearningOnlineModel implements RankingModel {

	private final Random randomGenerator = new Random(31);
	
	private int numClassifiers = 1;
	private int numFeatures = 1000;
	
	private FeaturesCoordinator coordinator;
	
	private Combiner combiner;// = new SumCombiner();//new MaxCombiner();//
	
	ALTextClassifier<LinearBinaryOnlineAlgorithm<Long>, Pair<Map<Integer, DataObject<Map<Long, Double>>>,Map<Integer, Boolean>>,Pair<DataObject<Map<Long, Double>>,Boolean>> tctest;

	ClassifiersGenerator<LinearBinaryOnlineAlgorithm<Long>, Pair<Map<Integer, DataObject<Map<Long, Double>>>,Map<Integer, Boolean>>,Pair<DataObject<Map<Long, Double>>,Boolean>> cg;
	
	ClassifierCreator<LinearBinaryOnlineAlgorithm<Long>, Pair<Map<Integer, DataObject<Map<Long, Double>>>,Map<Integer, Boolean>>> cc;
	
	private int numEpochs;
	private String regularization;
	private double lambda;
	private boolean useTerms;
	private LinearBinaryOnlineAlgorithm<Long> termsWeightsModel;
	private boolean termsAreQueries;
	
	private ActiveLearningOnlineModel(){
		
	}
	
	public ActiveLearningOnlineModel(List<String> docs, List<String> relevantDocs, FeaturesCoordinator coordinator, int numEpochs, Combiner combiner, String regularization, double lambda, boolean useTerms, boolean termsAreQueries) throws IOException, Exception {
		this.termsAreQueries = termsAreQueries;
		this.useTerms = useTerms;
		this.regularization = regularization;
		this.lambda = lambda;
		this.combiner = combiner;
		this.coordinator = coordinator;
		this.numEpochs = numEpochs;
		cg = new DifferentDataClassifierGenerator<LinearBinaryOnlineAlgorithm<Long>, Pair<Map<Integer, DataObject<Map<Long, Double>>>,Map<Integer, Boolean>>,Pair<DataObject<Map<Long, Double>>,Boolean>>(numClassifiers,numFeatures);
		trainModel(docs,relevantDocs);
		
	}

	private void trainModel(List<String> docs, List<String> relevantDocs) throws IOException, Exception {
		
		Data<Pair<Map<Integer, DataObject<Map<Long, Double>>>, Map<Integer, Boolean>>, Pair<DataObject<Map<Long, Double>>,Boolean>> data = createStructure(docs,relevantDocs);
		
		cc = new OnlineClassifierCreator(numEpochs,regularization,lambda);
		
		if (useTerms){
			termsWeightsModel = cc.createClassifier(data.getInstances());
		}
		
		List<LinearBinaryOnlineAlgorithm<Long>> classifiers = cg.train(data,cc);
		
		List<Pair<Set<Long>,LinearBinaryOnlineAlgorithm<Long>>> cl = new ArrayList<Pair<Set<Long>,LinearBinaryOnlineAlgorithm<Long>>>();
		
		for (int i = 0; i < classifiers.size(); i++) {
			cl.add(new Pair<Set<Long>, LinearBinaryOnlineAlgorithm<Long>>(null, classifiers.get(i)));
		}
		
		tctest = new OnlineClassifier(coordinator,cl,combiner);
		
	}

	private Data<Pair<Map<Integer, DataObject<Map<Long, Double>>>, Map<Integer, Boolean>>, Pair<DataObject<Map<Long, Double>>,Boolean>> createStructure(Collection<String> docs, Collection<String> relevantDocs) throws IOException, Exception {
		
		ALTextClassifier<LinearBinaryOnlineAlgorithm<Long>, Pair<Map<Integer, DataObject<Map<Long, Double>>>,Map<Integer, Boolean>>,Pair<DataObject<Map<Long, Double>>,Boolean>> tc = new OnlineClassifier(coordinator);//new WekaCoordinatorClassifier(coordinator);//new WekaClassifier("tokenize, ssplit, pos, lemma");
		
		List<String[]> texts = new ArrayList<String[]>();
		
		List<String> useless = new ArrayList<String>(docs);
		
		List<String> useful = new ArrayList<String>(relevantDocs);
		
		useless.removeAll(relevantDocs);
		
//		making it even.
		
		//Collections.shuffle(useless); //If I don't do shuffle, I obtain the useless documents that are close to be useful... might be interesting to see
		
		if (useless.size() > useful.size()){
			Collections.shuffle(useless,randomGenerator);
			useless = useless.subList(0, useful.size());
		}else if (useful.size() > useless.size()){
			Collections.shuffle(useful,randomGenerator);
			useful = useful.subList(0, useless.size());
		}
		
		texts.add(useless.toArray(new String[useless.size()]));
		
		texts.add(useful.toArray(new String[useful.size()]));
		
		String[] classes = new String[]{"0","1"};

		return tc.createDataStructure(classes, texts);
				
	}

	private Map<String, Double> getScores(Set<String> collection) throws Exception {
		
		Map<String,Double> ret = new HashMap<String, Double>();
		
		for (String string : collection) {
		
			ret.put(string, tctest.getConfidenceValue(string, "1"));
			
		}
		
		return ret;
	}

	public Map<String,Double> getScores(Set<String> collection, IndexConnector conn, boolean independent) throws Exception{
		
		if (!termsAreQueries){
			return getScores(collection);
		}
		
		return tctest.getScores(collection,conn,independent);
		
	}
	
	public void updateModel(Collection<String> docs,
			Collection<String> relevantDocs, int maxInstances) throws Exception {
		
		Data<Pair<Map<Integer, DataObject<Map<Long, Double>>>, Map<Integer, Boolean>>, Pair<DataObject<Map<Long, Double>>,Boolean>> data = createStructure(docs,relevantDocs);
		
		if (useTerms){
			if (termsWeightsModel == null){
				termsWeightsModel = cc.createClassifier(data.getInstances());
			}else{
				cc.updateClassifier(termsWeightsModel, data.getInstances());
			}
		}
		
		List<Pair<Set<Long>,LinearBinaryOnlineAlgorithm<Long>>> classifiers = cg.update(data,cc);
		
		tctest = new OnlineClassifier(coordinator,classifiers,combiner);
		
	}

	public Map<pt.utl.ist.online.learning.utils.Pair<String, String>, Double> getTermWeights() {
		
		Map<Long,Double> weightVector = termsWeightsModel.getWeightVectors();
		System.out.println(weightVector.size() + " features.");
		Map<pt.utl.ist.online.learning.utils.Pair<String,String>, Double> termWeights = new HashMap<pt.utl.ist.online.learning.utils.Pair<String,String>, Double>();
		for(Entry<Long,Double> entry : weightVector.entrySet()){
			pt.utl.ist.online.learning.utils.Pair<String,String> term = coordinator.getTerm(entry.getKey());
			if(term!=null){
				termWeights.put(term, entry.getValue());
			}
		}
		
		return termWeights;
	}

	@Override
	public RankingModel getTempCopyModel() {		
		ActiveLearningOnlineModel thisa = new ActiveLearningOnlineModel();
		
		thisa.termsAreQueries = termsAreQueries;
		thisa.useTerms = useTerms;
		thisa.regularization = regularization;
		thisa.lambda = lambda;
		thisa.combiner = combiner;
		thisa.coordinator = coordinator;
		thisa.numEpochs = numEpochs;
		
		thisa.cc = new OnlineClassifierCreator(numEpochs,regularization,lambda);
		
		if (useTerms){
			thisa.termsWeightsModel = termsWeightsModel.copy();
		}

		thisa.tctest = tctest.copy();
		
		thisa.cg =new DifferentDataClassifierGenerator<LinearBinaryOnlineAlgorithm<Long>, Pair<Map<Integer, DataObject<Map<Long, Double>>>,Map<Integer, Boolean>>,Pair<DataObject<Map<Long, Double>>,Boolean>>(numClassifiers,numFeatures,thisa.tctest.getClassifiers());

		
		return thisa;
		
	}

	@Override
	public Map<Long, Double> getWeightVector() {
		//return termsWeightsModel.getWeightVectors();
		return ((OnlineClassifier)tctest).getWeightVectors(true);
	}

	@Override
	public double getModelSimilarity(RankingModel copyModel) {
		if(copyModel instanceof ActiveLearningOnlineModel){
			return ((OnlineClassifier)tctest).getModelSimilarity((OnlineClassifier)((ActiveLearningOnlineModel)copyModel).tctest);
		}else{
			return 0;
		}
	}

}
