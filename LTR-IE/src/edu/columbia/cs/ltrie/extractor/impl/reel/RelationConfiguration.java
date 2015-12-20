package edu.columbia.cs.ltrie.extractor.impl.reel;

import java.io.File;
import java.io.IOException;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opennlp.tools.util.InvalidFormatException;

import edu.columbia.cs.ref.algorithm.feature.generation.FeatureGenerator;
import edu.columbia.cs.ref.algorithm.feature.generation.impl.EntityBasedChunkingFG;
import edu.columbia.cs.ref.algorithm.feature.generation.impl.OpenNLPPartOfSpeechFG;
import edu.columbia.cs.ref.algorithm.feature.generation.impl.OpenNLPTokenizationFG;
import edu.columbia.cs.ref.algorithm.feature.generation.impl.SpansToStringsConvertionFG;
import edu.columbia.cs.ref.model.Span;
import edu.columbia.cs.ref.model.StructureConfiguration;
import edu.columbia.cs.ref.model.core.impl.BagOfNGramsKernel;
import edu.columbia.cs.ref.model.core.impl.DependencyGraphsKernel;
import edu.columbia.cs.ref.model.core.impl.OpenInformationExtractionCore;
import edu.columbia.cs.ref.model.core.impl.ShortestPathKernel;
import edu.columbia.cs.ref.model.core.impl.SubsequencesKernel;
import edu.columbia.cs.ref.model.feature.impl.SequenceFS;
import edu.columbia.cs.ref.model.re.Model;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import etxt2db.api.ClassificationExecutor;
import etxt2db.api.ClassificationModel;
import etxt2db.serialization.ClassificationModelSerializer;

public class RelationConfiguration {

	private static final int PERSON = 1;
	private static final int LOCATION = 2;
	private static final int MANMADEDISASTER = 3;
	private static final int NATURALDISASTER = 4;
	private static final int CHARGE = 5;
	private static final int POLITICALEVENT = 6;
	private static final int CAREER = 7;
	
	private static final int PCCONF = 1;
	private static final int NDCONF = 2;
	private static final int MMDCONF = 3;
	private static final int PTCONF = 4;
	private static final int VRCONF = 5;
	private static final int IATCONF = 6;
	
	public static final int MANMADEDISASTEREXP = 1;
	public static final int NATURALDISASTEREXP = 2;
	public static final int POLITICALEVENTEXP = 3;
	public static final int PERSONLOCATION = 4;
	public static final int CHARGEEXP = 5;
	public static final int CAREEREXP = 6;
	private static Map<String, Integer> tagsTable;
	
	private static final String LOC_TAG = "LOCATION";
	private static final String MMD_TAG = "MANMADEDISASTER";
	private static final String ND_TAG = "NATURALDISASTER";
	private static final String PER_TAG = "PERSON";
	private static final String PE_TAG = "POLITICALEVENT";
	private static final String CA_TAG = "CAREER";
	private static final String IAT_TAG = "CHARGE";
	
	public static Set<String> getTags(int relationConf) {

		Set<String> ret = new HashSet<String>();
		
		//has the extractorId,entityId
		if (relationConf == MMDCONF){ //ManMadeDisaster
			
			ret.add(LOC_TAG);
			ret.add(MMD_TAG);
			
		}
		if (relationConf == 2){ //NaturalDisaster
			
			ret.add(LOC_TAG);
			ret.add(ND_TAG);
			
		}
		if (relationConf == 5){ //VotingResult
			
			ret.add(PER_TAG);
			ret.add(PE_TAG);
			
		}
		if (relationConf == 4){ //PersonTravel
			
			ret.add(PER_TAG);
			ret.add(LOC_TAG);
			
		}
		if (relationConf == 1){ //PersonCareer
			ret.add(PER_TAG);
			ret.add(CA_TAG);
			
		}
		if (relationConf == 6){ //Indictment-Arrest-Trial
			
			ret.add(PER_TAG);
			ret.add(IAT_TAG);
			
		}

		return ret;
		
	}
	
	public static Integer getForExtractor(int entity) {
		switch (entity){
		case PERSON:
			return PERSONLOCATION;
		case LOCATION:
			return PERSONLOCATION;
		case MANMADEDISASTER:
			return MANMADEDISASTEREXP;
		case NATURALDISASTER:
			return NATURALDISASTEREXP;
		case CHARGE:
			return CHARGEEXP;
		case POLITICALEVENT:
			return POLITICALEVENTEXP;
		case CAREER:
			return CAREEREXP;
		default:
			return -1;
		}
	}
	
	public static EntityExtractor createEntityExtractor(String entityModels, int entity) throws ClassCastException, IOException, ClassNotFoundException {

		String prefix = entityModels;

		ClassificationModelSerializer serial = new ClassificationModelSerializer();
		
		if (entity == RelationConfiguration.MANMADEDISASTEREXP){
			
			//Load ManMadeDisaster
			
			String relation = "ManMadeDisaster";
			int split = 5;
			String technique = "CRF";
			int informationExtractionId = 3;
			
			ClassificationModel mmd = serial.deserializeClassificationModel(prefix+relation+"/"+split+"/" + technique + "-reloaded.bin");
			ClassificationExecutor mmdexec = new ClassificationExecutor();
		
			return new ClassificationBasedExtractor(mmdexec,mmd,informationExtractionId,getTagsTable(),getTags(relation));
		
		} else if (entity == RelationConfiguration.NATURALDISASTEREXP){
		//Load NaturalDisaster
		
			String relation = "NaturalDisaster";
			int split = 5;
			String technique = "SVM";
			int informationExtractionId = 4;
			
			Class klass = libsvm.svm_model.class;

		    CodeSource codeSource = klass.getProtectionDomain().getCodeSource();

		    if ( codeSource != null) {

		        System.out.println("source: " + codeSource.getLocation());

		    }
			
			ClassificationModel nd = serial.deserializeClassificationModel(prefix+relation+"/"+split+"/" + technique + "-reloaded.bin");
			ClassificationExecutor ndexec = new ClassificationExecutor();
		
			return new ClassificationBasedExtractor(ndexec, nd, informationExtractionId, getTagsTable(),getTags(relation));
		} else if (entity == RelationConfiguration.POLITICALEVENTEXP){
		//Load VotingResult
		
			String relation = "VotingResult";
			int split = 5;
			String technique = "CRF";
			int informationExtractionId = 3;
		
			ClassificationModel vr = serial.deserializeClassificationModel(prefix+relation+"/"+split+"/" + technique + "-reloaded.bin");
			ClassificationExecutor vrexec = new ClassificationExecutor();
		
			return new ClassificationBasedExtractor(vrexec, vr, informationExtractionId, getTagsTable(),getTags(relation));
		} else if (entity == RelationConfiguration.PERSONLOCATION){
			
			//Load Person-Location
			String relation = "Person-Location";
			int informationExtractionId = 15;
			String classifier = "model/english.all.3class.distsim.crf.ser.gz";
			
			return new StanfordNLPBasedExtractor(informationExtractionId, getTagsTable(),getTags(relation), CRFClassifier.getClassifier(new File(classifier)));

			
		}  else if (entity == RelationConfiguration.CHARGEEXP){
			
			//Load Indictment-Arrest-Trial
			String relation = "Indictment-Arrest-Trial";
			int split = 5;
			String technique = "CRF";
			int informationExtractionId = 3;
			
			ClassificationModel vr = serial.deserializeClassificationModel(prefix+relation+"/"+split+"/" + technique + "-reloaded.bin");
			ClassificationExecutor vrexec = new ClassificationExecutor();
		
			return new ClassificationBasedExtractor(vrexec, vr, informationExtractionId, getTagsTable(),getTags(relation));
			
		}
		
		else if (entity == RelationConfiguration.CAREEREXP){
			
			//Load Career
			String relation = "PersonCareer";
			int split = 5;
			String technique = "CRF";
			int informationExtractionId = 3;
			
			ClassificationModel vr = serial.deserializeClassificationModel(prefix+relation+"/"+split+"/" + technique + "-reloaded.bin");
			ClassificationExecutor vrexec = new ClassificationExecutor();
		
			return new ClassificationBasedExtractor(vrexec, vr, informationExtractionId, getTagsTable(),getTags(relation));
			
		}
		
		//End
		
		return null;
		
	}
	
	private static List<String> getTags(String relation) {
		
		if (relation.equals("ManMadeDisaster"))
			return Arrays.asList(new String[]{"MANMADEDISASTER"});
		if (relation.equals("NaturalDisaster"))
			return Arrays.asList(new String[]{"NATURALDISASTER"});
		if (relation.equals("Indictment-Arrest-Trial"))
			return Arrays.asList(new String[]{"CHARGE"});
		if (relation.equals("VotingResult"))
			return Arrays.asList(new String[]{"POLITICALEVENT"});
		if (relation.equals("PersonCareer"))
			return Arrays.asList(new String[]{"CAREER"});
		if (relation.equals("Person-Location"))
			return Arrays.asList(new String[]{"PERSON","LOCATION"});
		return null;
	}
		
	private static final int MANMADEDISASTERRELATION = 3;
	private static final int NATURALDISASTERRELATION = 2;
	private static final int VOTINGRESULTRELATION = 5;
	private static final int PERSONTRAVELRELATION = 4;
	private static final int CHARGERELATION = 6;
	private static final int PERSONCAREERRELATION = 1;
	
	public static String getRelationName(int relation) {
		
		if (relation == MANMADEDISASTERRELATION){
			return MMDNAME;
		}
		if (relation == NATURALDISASTERRELATION){
			return NDNAME;
		}
		if (relation == VOTINGRESULTRELATION){
			return VRNAME;
		}
		if (relation == PERSONTRAVELRELATION){
			return PTNAME;
		}
		if (relation == PERSONCAREERRELATION){
			return PCNAME;
		}
		if (relation == CHARGERELATION){
			return IATNAME;
		}
		
		return null;
		
	}

	
	public static Map<String,Integer> getTagsTable() {

		if (tagsTable == null){
            tagsTable = new HashMap<String, Integer>();
            tagsTable.put("PERSON",1);
            tagsTable.put("LOCATION",2);
            tagsTable.put("MANMADEDISASTER",3);
            tagsTable.put("NATURALDISASTER",4);
            tagsTable.put("CHARGE",5);
            tagsTable.put("POLITICALEVENT",6);
            tagsTable.put("CAREER",7);
        }

		return tagsTable;
	}
	
	public static Model generateRelationExtractionSystem(String pathModels, int informationExtractionId, int relationConf) {
		
		String fileModel = pathModels + getRESFileModel(informationExtractionId,getRelationshipType(relationConf));
		
		System.out.println("Opening " + fileModel);
		
		try {
			return(Model) edu.columbia.cs.ref.tool.io.SerializationHelper.read(fileModel);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String getRESFileModel(int informationExtractionId,
			int relationshipType) {
		
		String ext = getExtrctor(informationExtractionId);
		
		
		
		String rel = getRelationNameFromType(relationshipType);
		
		return rel + "-" + ext + "-CR-5.model";
		
	}

	public static String getRelationNameFromType(int relation) {
		
		if (relation == 3){
			return MMDNAME;
		}
		if (relation == 2){
			return NDNAME;
		}
		if (relation == 5){
			return VRNAME;
		}
		if (relation == 4){
			return PTNAME;
		}
		if (relation == 1){
			return PCNAME;
		}
		if (relation == 6){
			return IATNAME;
		}
		
		return null;
		
	}
	
	private static final String MMDNAME = "ManMadeDisaster";
	private static final String NDNAME = "NaturalDisaster";
	private static final String VRNAME = "VotingResult";
	private static final String PTNAME = "PersonTravel";
	private static final String PCNAME = "PersonCareer";
	private static final String IATNAME = "Indictment-Arrest-Trial";

	
	private static String getExtrctor(int informationExtractionId) {
		String ext = "";
		switch (informationExtractionId) {
		case 3:
			ext = "BONG";
			break;
		case 1:
			ext = "SPK";
			break;
		case 2:
			ext = "SSK";
			break;
		case 4:
			ext = "DG";
			break;
		default:
			break;
		}
		return ext;
	}

	private static int getRelationshipType(int relationConf) {
		
		if (relationConf == MMDCONF){ //ManMadeDisaster
			return 3;
		}
		if (relationConf == NDCONF){ //NaturalDisaster
			return 2;
		}
		if (relationConf == VRCONF){ //VotingResult
			return 5;
		}
		if (relationConf == PTCONF){ //PersonTravel
			return 4;
		}
		if (relationConf == PCCONF){ //PersonCareer
			return 1;
		}
		if (relationConf == IATCONF){ //Indictment-Arrest-Trial
			return 6;
		}
		
		return -1;
		
	}

	public static int[][] getEntities(int relationConf) { //returns the most popular first
		//has the extractorId,entityId
		if (relationConf == MMDCONF){ //ManMadeDisaster
			return new int[][]{new int[]{15, 2},new int[]{3, 3}};
		}
		if (relationConf == NDCONF){ //NaturalDisaster
			return new int[][]{new int[]{15, 2},new int[]{4, 4}};
		}
		if (relationConf == VRCONF){ //VotingResult
			return new int[][]{new int[]{15, 1},new int[]{3, 6}};
		}
		if (relationConf == PTCONF){ //PersonTravel
			return new int[][]{new int[]{15, 1},new int[]{15, 2}};
		}
		if (relationConf == PCCONF){ //PersonCareer
			return new int[][]{new int[]{15, 1},new int[]{3, 7}};
		}
		if (relationConf == IATCONF){ //Indictment-Arrest-Trial
			return new int[][]{new int[]{15, 1},new int[]{3, 5}};
		}
		
		return null;
	}
	
	public static StructureConfiguration generateStructureConfiguration(
			int infEsys) throws InvalidFormatException, IOException {

		FeatureGenerator<SequenceFS<Span>> tokenizer = new OpenNLPTokenizationFG("en-token.bin");
		FeatureGenerator<SequenceFS<Span>> fgChunk = new EntityBasedChunkingFG(tokenizer);
		FeatureGenerator<SequenceFS<String>> fgChuckString = new SpansToStringsConvertionFG(fgChunk);
		FeatureGenerator<SequenceFS<String>> fgPOS = new OpenNLPPartOfSpeechFG("en-pos-maxent.bin",fgChuckString);
		StructureConfiguration sc;
		switch (infEsys) {
		case SPK:
			
			sc = new StructureConfiguration(new ShortestPathKernel());
			
			sc.addFeatureGenerator(fgPOS);
			
			break;

		case SSK:
			
			sc = new StructureConfiguration(new SubsequencesKernel());
			
			sc.addFeatureGenerator(fgPOS);
			
			break;
			
		case BNG:
			
			sc = new StructureConfiguration(new BagOfNGramsKernel());
			
			sc.addFeatureGenerator(fgPOS);
			
			break;
			
		case DG:
			
			sc = new StructureConfiguration(new DependencyGraphsKernel());
			
			sc.addFeatureGenerator(fgPOS);
			
			break;
			
		case OIE:
			
			sc = new StructureConfiguration(new OpenInformationExtractionCore());
		
			break;
			
		default:
			
			return null;
			
		}
	
		return sc;
		
	}
	
	private static final int SPK = 1;
	private static final int SSK = 2;
	private static final int BNG = 3;
	private static final int DG = 4;
	private static final int OIE = 5;

	
	public static String getType(int relationConf) {
		
		switch (relationConf) {
		case MMDCONF:
			
			return "ManMadeDisaster";

		case NDCONF:
			
			return "NaturalDisaster";
			
		case PCCONF:
			
			return "PersonCareer";

		case PTCONF:
			
			return "PersonTravel";

		case IATCONF:
			
			return "Indictment-Arrest-Trial";

		case VRCONF:
			
			return "VotingResult";
			
		default:
			return null;
		}
		
	}
	
	
}
