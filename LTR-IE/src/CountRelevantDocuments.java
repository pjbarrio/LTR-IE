import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.AdditiveFileSystemWrapping;


public class CountRelevantDocuments {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		String[] relationships = new String[]{"Indictment-Arrest-Trial", "VotingResult", "PersonCareer","NaturalDisaster", "ManMadeDisaster","OrgAff","Outbreaks"};
		int numPaths=672;
		String[] subPaths = new String[numPaths];
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format("%03d", i);
		}

		for (int i = 0; i < relationships.length; i++) {
			String relationship = relationships[i];
			String extractor = "Pablo-Sub-sequences";
			String resultsPath = "results" + relationship;
			
			AdditiveFileSystemWrapping extractWrapper = new AdditiveFileSystemWrapping();
			for(String subPath : subPaths){
				if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationship + ".data");
				}else{
					extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + ".data");
				}
			}
			
			Map<String, List<Tuple>> map = extractWrapper.getInternal();
			
			Map<String, Integer> mapI = new HashMap<String, Integer>();
			
			for (Entry<String, List<Tuple>> entry : map.entrySet()) {
				mapI.put(entry.getKey(), entry.getValue().size());
			}
			
			SerializationHelper.write("../ResourceSelectionIE/data/extraction/" + relationship + ".default.NYT.validation", mapI);
			
		}
		
		

	}

}
