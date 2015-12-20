package edu.columbia.cs.ltrie.extractor.impl.reel;


import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gdata.util.common.base.Pair;

import edu.columbia.cs.ref.model.CandidateSentence;
import edu.columbia.cs.ref.model.Document;

public class CachCandidateSentenceRunnable implements Runnable {

	private Document docContent;
	private CandidateSentenceGenerator candsentGenerator;
	private Set<CandidateSentence> set;
	private Map<Integer, String> entitiesTable;
	Map<Integer, List<Pair<Long, Pair<Integer, Integer>>>> entitiesMap;
	
	public CachCandidateSentenceRunnable(Document doc,CandidateSentenceGenerator candsentGenerator, Map<Integer, List<Pair<Long, Pair<Integer, Integer>>>> entitiesMap, Map<Integer, String> entitiesTable, Set<CandidateSentence> set) {
		this.docContent = doc;
		this.candsentGenerator = candsentGenerator;
		this.entitiesMap = entitiesMap;
		this.entitiesTable = entitiesTable;
		this.set = set;
	}

	@Override
	public void run() {
				
		Set<CandidateSentence> auxSet;
		
		try{
		
			auxSet = candsentGenerator.generateCandidateSentences(docContent, entitiesMap, entitiesTable);

		}catch (Exception e) {
			
			e.printStackTrace();
			System.out.println(docContent.getPath());
			System.out.println(docContent.getFilename());
			System.out.println(docContent.getEntities());
			System.out.println(docContent);
			System.err.println("Running anyway");
			auxSet = new HashSet<CandidateSentence>(0);
			System.exit(1);
		}
		
		set.addAll(auxSet);
		
	}

}