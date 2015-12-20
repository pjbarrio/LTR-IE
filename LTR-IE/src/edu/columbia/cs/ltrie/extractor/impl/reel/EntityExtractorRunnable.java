package edu.columbia.cs.ltrie.extractor.impl.reel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gdata.util.common.base.Pair;

import edu.columbia.cs.ref.model.Document;
import etxt2db.api.ClassificationExecutor.ClassifiedSpan;

class EntityExtractorRunnable implements Runnable{

	static long ids = (long)Math.random() * Long.MAX_VALUE;

	private EntityExtractor entityExtractor;
	private Document doc;
	private Map<Integer, List<Pair<Long, Pair<Integer, Integer>>>> ret;
	private Map<String, Integer> entityTable;

	public EntityExtractorRunnable(Document doc, EntityExtractor entityExtractor, Map<Integer, List<Pair<Long, Pair<Integer, Integer>>>> ret, Map<String,Integer> entityTable) {
		this.doc = doc;
		this.entityExtractor = entityExtractor;
		this.ret = ret;
		this.entityTable = entityTable;
	}
	@Override
	public void run() {

		Map<String, List<ClassifiedSpan>> map = new HashMap<String,List<ClassifiedSpan>>(0);

		Runnable t = new CachExtractRunnable(doc,entityExtractor, map);

		t.run();		

		for (Entry<String, List<ClassifiedSpan>> entry : map.entrySet()){
			synchronized (ret) {
				ret.put(entityTable.get(entry.getKey()),generateList(entry.getValue()));
			}

		}

	}

	private List<Pair<Long, Pair<Integer, Integer>>> generateList(
			List<ClassifiedSpan> value) {

		List<Pair<Long, Pair<Integer, Integer>>> ret = new ArrayList<Pair<Long, Pair<Integer, Integer>>>(value.size());

		for(int i = 0; i < value.size() ; i++){
			ret.add(generateElement(value.get(i)));
		}

		return ret;

	}

	private Pair<Long, Pair<Integer, Integer>> generateElement(
			ClassifiedSpan classifiedSpan) {
		return new Pair<Long,Pair<Integer,Integer>>(ids++,new Pair<Integer,Integer>(classifiedSpan.getStart(),classifiedSpan.getEnd()));

	}

}